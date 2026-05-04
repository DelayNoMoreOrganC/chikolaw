"""
OCR处理模块 - 专业增强版
针对扫描件PDF优化，提供多种免费OCR方案
"""

import os
import io
import tempfile
from typing import List, Dict, Optional, Tuple, Any

import fitz  # PyMuPDF
import pdfplumber
from PIL import Image, ImageEnhance, ImageFilter, ImageOps
import pytesseract
from pdf2image import convert_from_path

import sys
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

class OCRProcessor:
    """OCR处理器类 - 专业增强版"""

    def __init__(
        self,
        tesseract_cmd: Optional[str] = None,
        dpi: int = 400,  # 提高到400
        language: str = "chi_sim+eng"
    ):
        """
        初始化OCR处理器

        Args:
            tesseract_cmd: Tesseract可执行文件路径（Windows需要）
            dpi: OCR图像分辨率（提高到400）
            language: OCR语言设置
        """
        # 配置Tesseract路径
        if tesseract_cmd:
            pytesseract.pytesseract.tesseract_cmd = tesseract_cmd

        self.dpi = dpi
        self.language = language
        self.temp_dir = None

    def __enter__(self):
        """上下文管理器入口"""
        self.temp_dir = tempfile.mkdtemp()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """上下文管理器退出，清理临时文件"""
        if self.temp_dir and os.path.exists(self.temp_dir):
            import shutil
            shutil.rmtree(self.temp_dir, ignore_errors=True)

    def extract_text_from_pdf(
        self,
        pdf_file: bytes,
        max_pages: int = 50,
        use_ocr_fallback: bool = True,
        ocr_all_pages: bool = True
    ) -> Tuple[str, List[Image.Image]]:
        """
        从PDF提取文本（专业增强版）
        针对扫描件PDF优化

        Args:
            pdf_file: PDF文件字节流
            max_pages: 最大处理页数
            use_ocr_fallback: 当文本提取失败时是否使用OCR兜底
            ocr_all_pages: 是否对所有页面进行OCR

        Returns:
            Tuple[str, List[Image.Image]]: (提取的文本, 页面图像列表)
        """
        all_text = []
        page_images = []

        try:
            # 先尝试提取文本层
            text_layer_text = self._extract_text_layer(pdf_file, max_pages)
            text_layer_text = self._normalize_text(text_layer_text)

            # 如果文本层为空或很少，使用OCR
            if text_layer_text:
                all_text.append(text_layer_text)

            if use_ocr_fallback and (ocr_all_pages or len(text_layer_text) < 100):
                print("[WARNING] 文本层为空或内容过少，启用OCR识别")

                # 将PDF转换为图像
                page_images = self._pdf_to_images(pdf_file, max_pages)
                ocr_text_parts = []

                # 对每一页进行增强OCR识别
                for i, image in enumerate(page_images):
                    try:
                        print(f"正在处理第{i+1}页...")

                        ocr_texts = self._extract_text_from_image(image)

                        # 选择结果最好的
                        best_text = max(ocr_texts, key=lambda x: len(x.strip()))
                        best_text = self._normalize_text(best_text)

                        if best_text.strip():
                            ocr_text_parts.append(f"--- 第{i+1}页 ---\n{best_text}")
                            print(f"  [OK] 第{i+1}页OCR成功，识别 {len(best_text.strip())} 个字符")
                        else:
                            print(f"  [FAIL] 第{i+1}页OCR识别失败")

                    except Exception as e:
                        print(f"  [ERROR] 第{i+1}页处理失败: {str(e)}")

                if ocr_text_parts:
                    all_text.append("\n\n".join(ocr_text_parts))

            else:
                # 文本层有内容
                print(f"[OK] 成功提取文本层，{len(text_layer_text)} 个字符")

                # 生成页面图像
                page_images = self._pdf_to_images(pdf_file, max_pages)

            # 合并所有文本
            combined_text = self._normalize_text("\n\n".join(part for part in all_text if part))

            return combined_text, page_images

        except Exception as e:
            raise Exception(f"PDF解析失败: {str(e)}")

    def extract_text_from_image_bytes(self, image_file: bytes) -> Tuple[str, List[Image.Image]]:
        """
        从图片字节中提取文本，适用于系统截图或拍照件。
        """
        try:
            image = Image.open(io.BytesIO(image_file))
            ocr_texts = self._extract_text_from_image(image)
            best_text = max(ocr_texts, key=lambda item: len((item or "").strip()), default="")
            return self._normalize_text(best_text), [image]
        except Exception as e:
            raise Exception(f"图片OCR处理失败: {str(e)}")

    def _extract_text_layer(self, pdf_file: bytes, max_pages: int = 50) -> str:
        """
        提取PDF的文本层（优先使用）

        Args:
            pdf_file: PDF文件字节流

        Returns:
            str: 提取的文本
        """
        all_text = []

        try:
            # 使用pdfplumber提取
            with pdfplumber.open(io.BytesIO(pdf_file)) as pdf:
                total_pages = min(len(pdf.pages), max_pages)

                for i in range(total_pages):
                    page = pdf.pages[i]
                    text = page.extract_text()

                    if text and text.strip():
                        all_text.append(text)

        except Exception as e:
            print(f"文本层提取失败: {str(e)}")

        # 使用PyMuPDF作为补充
        try:
            doc = fitz.open(stream=pdf_file, filetype="pdf")
            total_pages = min(len(doc), max_pages)

            for i in range(total_pages):
                page = doc[i]
                text = page.get_text("text", flags=fitz.TEXT_PRESERVE_WHITESPACE | fitz.TEXT_PRESERVE_LIGATURES)

                if text and text.strip():
                    all_text.append(text)

            doc.close()
        except Exception as e:
            print(f"PyMuPDF提取失败: {str(e)}")

        return "\n\n".join(all_text)

    def _ocr_with_standard_preprocess(self, image: Image.Image) -> str:
        """
        标准预处理OCR

        Args:
            image: PIL图像对象

        Returns:
            str: OCR识别的文本
        """
        try:
            # 转灰度
            if image.mode != 'L':
                image = image.convert('L')

            # 调整大小（2倍）
            image = image.resize((image.width * 2, image.height * 2), Image.LANCZOS)

            # 增强对比度
            enhancer = ImageEnhance.Contrast(image)
            image = enhancer.enhance(2.0)

            # OCR识别
            custom_config = r'--oem 3 --psm 6 -c preserve_interword_spaces=1'
            text = pytesseract.image_to_string(
                image,
                lang=self.language,
                config=custom_config
            )

            return text
        except Exception as e:
            print(f"标准OCR失败: {str(e)}")
            return ""

    def _ocr_with_enhanced_preprocess(self, image: Image.Image) -> str:
        """
        增强预处理OCR（针对模糊或低质量扫描件）

        Args:
            image: PIL图像对象

        Returns:
            str: OCR识别的文本
        """
        try:
            # 转灰度
            if image.mode != 'L':
                image = image.convert('L')

            # 调整大小（3倍）
            image = image.resize((image.width * 3, image.height * 3), Image.LANCZOS)

            # 增强对比度
            enhancer = ImageEnhance.Contrast(image)
            image = enhancer.enhance(3.0)

            # 锐化
            enhancer = ImageEnhance.Sharpness(image)
            image = enhancer.enhance(2.0)

            # 去噪
            image = image.filter(ImageFilter.MedianFilter(size=3))

            # 自适应阈值二值化
            image = self._adaptive_threshold(image)

            # 反色（如果背景是深色）
            if self._is_dark_background(image):
                image = ImageOps.invert(image)

            # OCR识别（不做字符白名单限制，避免中文字段被误过滤）
            custom_config = r'--oem 3 --psm 4 -c preserve_interword_spaces=1'
            text = pytesseract.image_to_string(
                image,
                lang=self.language,
                config=custom_config
            )

            return text
        except Exception as e:
            print(f"增强OCR失败: {str(e)}")
            return ""

    def _ocr_with_sparse_text_preprocess(self, image: Image.Image) -> str:
        """
        稀疏文本OCR，适合表格、票据、扫描质量较差的页面

        Args:
            image: PIL图像对象

        Returns:
            str: OCR识别的文本
        """
        try:
            processed = image
            if processed.mode != 'L':
                processed = processed.convert('L')

            processed = processed.resize(
                (processed.width * 2, processed.height * 2),
                Image.LANCZOS
            )
            processed = ImageEnhance.Contrast(processed).enhance(2.5)
            processed = processed.filter(ImageFilter.SHARPEN)

            custom_config = r'--oem 3 --psm 11 -c preserve_interword_spaces=1'
            text = pytesseract.image_to_string(
                processed,
                lang=self.language,
                config=custom_config
            )
            return text
        except Exception as e:
            print(f"稀疏文本OCR失败: {str(e)}")
            return ""

    def _extract_text_from_image(self, image: Image.Image) -> List[str]:
        """
        对单页图像尝试多种OCR策略，返回候选结果

        Args:
            image: PIL图像对象

        Returns:
            List[str]: 候选文本列表
        """
        results = [
            self._ocr_with_standard_preprocess(image),
            self._ocr_with_enhanced_preprocess(image),
            self._ocr_with_sparse_text_preprocess(image),
        ]
        return [result for result in results if result is not None]

    def _normalize_text(self, text: str) -> str:
        """
        统一清理OCR输出，减少无意义空白

        Args:
            text: 原始文本

        Returns:
            str: 清理后的文本
        """
        if not text:
            return ""

        lines = [line.rstrip() for line in text.splitlines()]
        cleaned_lines = []
        previous_blank = False

        for line in lines:
            if not line.strip():
                if previous_blank:
                    continue
                previous_blank = True
                cleaned_lines.append("")
                continue

            previous_blank = False
            cleaned_lines.append(line)

        return "\n".join(cleaned_lines).strip()

    def _adaptive_threshold(self, image: Image.Image) -> Image.Image:
        """
        自适应阈值二值化

        Args:
            image: PIL图像对象

        Returns:
            Image.Image: 二值化后的图像
        """
        try:
            # 使用PIL的point方法进行自适应阈值
            threshold = 140
            image = image.point(lambda x: 0 if x < threshold else 255)
            return image
        except:
            return image

    def _is_dark_background(self, image: Image.Image) -> bool:
        """
        判断是否为深色背景

        Args:
            image: PIL图像对象

        Returns:
            bool: 是否为深色背景
        """
        try:
            # 计算平均亮度
            grayscale = image.convert('L')
            histogram = grayscale.histogram()
            pixels = sum(histogram[0][:128])  # 暗色像素数量
            total = sum(histogram[0])

            # 如果暗色像素超过60%，认为是深色背景
            return pixels / total > 0.6
        except:
            return False

    def _pdf_to_images(
        self,
        pdf_file: bytes,
        max_pages: int = 50
    ) -> List[Image.Image]:
        """
        将PDF转换为图像列表（高DPI）

        Args:
            pdf_file: PDF文件字节流
            max_pages: 最大转换页数

        Returns:
            List[Image.Image]: PIL图像对象列表
        """
        try:
            # 保存临时PDF文件
            temp_pdf_path = os.path.join(self.temp_dir, "temp.pdf")
            with open(temp_pdf_path, 'wb') as f:
                f.write(pdf_file)

            # 转换为图像（高DPI）
            images = convert_from_path(
                temp_pdf_path,
                dpi=self.dpi,
                first_page=1,
                last_page=max_pages
            )

            return images

        except Exception as e:
            print(f"pdf2image转换失败: {str(e)}")
            # 尝试使用PyMuPDF渲染
            return self._pdf_to_images_fitz(pdf_file, max_pages)

    def _pdf_to_images_fitz(
        self,
        pdf_file: bytes,
        max_pages: int = 50
    ) -> List[Image.Image]:
        """
        使用PyMuPDF将PDF转换为图像（备用方法）

        Args:
            pdf_file: PDF文件字节流
            max_pages: 最大转换页数

        Returns:
            List[Image.Image]: PIL图像对象列表
        """
        images = []
        try:
            doc = fitz.open(stream=pdf_file, filetype="pdf")
            total_pages = min(len(doc), max_pages)

            for i in range(total_pages):
                page = doc[i]
                # 渲染为 pixmap（高倍缩放）
                zoom = 4  # 4倍缩放
                mat = fitz.Matrix(zoom, zoom)
                pix = page.get_pixmap(matrix=mat, alpha=False)

                # 转换为PIL Image
                img_data = pix.tobytes("png")
                img = Image.open(io.BytesIO(img_data))
                images.append(img)

            doc.close()
            return images

        except Exception as e:
            print(f"PyMuPDF渲染失败: {str(e)}")
            return []

    def extract_text_with_layout(
        self,
        pdf_file: bytes,
        max_pages: int = 50
    ) -> List[Dict[str, Any]]:
        """
        提取带布局信息的文本（保留位置信息）

        Args:
            pdf_file: PDF文件字节流
            max_pages: 最大处理页数

        Returns:
            List[Dict]: 每页的文本块信息
        """
        layout_data = []

        try:
            doc = fitz.open(stream=pdf_file, filetype="pdf")
            total_pages = min(len(doc), max_pages)

            for i in range(total_pages):
                page = doc[i]
                blocks = page.get_text("dict")["blocks"]

                page_data = {
                    "page_number": i + 1,
                    "width": page.rect.width,
                    "height": page.rect.height,
                    "blocks": []
                }

                for block in blocks:
                    if "lines" in block:  # 文本块
                        block_text = ""
                        for line in block["lines"]:
                            line_text = ""
                            for span in line["spans"]:
                                line_text += span["text"]
                            block_text += line_text + "\n"

                        if block_text.strip():
                            page_data["blocks"].append({
                                "type": "text",
                                "text": block_text.strip(),
                                "bbox": block["bbox"],
                                "size": block.get("size", 0)
                            })

                layout_data.append(page_data)

            doc.close()
            return layout_data

        except Exception as e:
            print(f"布局提取失败: {str(e)}")
            return []

    def get_page_images(
        self,
        pdf_file: bytes,
        pages: Optional[List[int]] = None,
        max_pages: int = 50
    ) -> Dict[int, Image.Image]:
        """
        获取指定页面的图像

        Args:
            pdf_file: PDF文件字节流
            pages: 页码列表（从0开始），None表示所有页面
            max_pages: 最大处理页数

        Returns:
            Dict[int, Image.Image]: 页码到图像的映射
        """
        images = {}

        try:
            all_images = self._pdf_to_images(pdf_file, max_pages)

            if pages is None:
                pages = list(range(len(all_images)))

            for page_num in pages:
                if page_num < len(all_images):
                    images[page_num] = all_images[page_num]

            return images

        except Exception as e:
            print(f"获取页面图像失败: {str(e)}")
            return {}


def create_ocr_processor(config: Dict) -> OCRProcessor:
    """
    工厂函数：根据配置创建OCR处理器

    Args:
        config: 配置字典

    Returns:
        OCRProcessor: OCR处理器实例
    """
    tesseract_config = config.get("tesseract", {})
    pdf_config = config.get("pdf", {})

    return OCRProcessor(
        tesseract_cmd=tesseract_config.get("tesseract_cmd"),
        dpi=pdf_config.get("dpi", 400),  # 提高默认DPI到400
        language=tesseract_config.get("language", "chi_sim+eng")
    )
