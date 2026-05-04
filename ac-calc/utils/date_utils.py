"""
日期处理工具模块
提供日期计算、天数统计、日期验证等功能
"""

from datetime import datetime, date, timedelta
from typing import Tuple, Union, List
from dateutil import parser
from dateutil.relativedelta import relativedelta

import sys
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from utils.constants import DateCalculationMethod, DayCountConvention


def parse_date(date_input: Union[str, date, datetime, None]) -> date:
    """
    解析日期输入，统一返回date对象

    Args:
        date_input: 日期输入，支持多种格式

    Returns:
        datetime.date: 解析后的日期对象

    Raises:
        ValueError: 日期格式无法解析
    """
    if date_input is None:
        raise ValueError("日期不能为空")

    if isinstance(date_input, date):
        # 如果是datetime类型，转为date
        if isinstance(date_input, datetime):
            return date_input.date()
        return date_input

    if isinstance(date_input, str):
        try:
            # 尝试多种格式解析
            return parser.parse(date_input).date()
        except Exception as e:
            raise ValueError(f"无法解析日期字符串: {date_input}，错误: {e}")

    raise ValueError(f"不支持的日期类型: {type(date_input)}")


def format_date(date_obj: Union[date, datetime], format_str: str = "%Y-%m-%d") -> str:
    """
    格式化日期为字符串

    Args:
        date_obj: 日期对象
        format_str: 格式化字符串

    Returns:
        str: 格式化后的日期字符串
    """
    if date_obj is None:
        return ""

    if isinstance(date_obj, datetime):
        date_obj = date_obj.date()

    return date_obj.strftime(format_str)


def calculate_days(
    start_date: Union[str, date, datetime],
    end_date: Union[str, date, datetime],
    method: DateCalculationMethod = DateCalculationMethod.HEAD_EXCLUSIVE
) -> int:
    """
    计算两个日期之间的天数

    Args:
        start_date: 起始日期
        end_date: 截止日期
        method: 计算方式
            - bilateral: 双边计算（算头又算尾，天数+1）
            - head_exclusive: 算头不算尾（默认）
            - tail_exclusive: 算尾不算头

    Returns:
        int: 计算天数

    Examples:
        >>> calculate_days("2024-01-01", "2024-01-02")
        1
        >>> calculate_days("2024-01-01", "2024-01-02", method="bilateral")
        2
    """
    start = parse_date(start_date)
    end = parse_date(end_date)

    # 计算基础天数
    delta = end - start
    base_days = delta.days

    if method == DateCalculationMethod.BILATERAL:
        # 双边：算头又算尾，天数+1
        return base_days + 1
    elif method == DateCalculationMethod.TAIL_EXCLUSIVE:
        # 算尾不算头：天数+1
        return base_days + 1
    else:
        # 算头不算尾（默认金融计算方式）
        return base_days


def calculate_interest_days(
    start_date: Union[str, date, datetime],
    end_date: Union[str, date, datetime],
    day_count_convention: DayCountConvention = DayCountConvention.ACTUAL_360
) -> Tuple[int, int]:
    """
    计算利息天数（根据不同的天数计算惯例）

    Args:
        start_date: 起始日期
        end_date: 截止日期
        day_count_convention: 天数计算规则
            - actual/360: 实际天数/360（默认）
            - actual/365: 实际天数/365
            - actual/actual: 实际天数/实际天数

    Returns:
        Tuple[int, int]: (实际天数, 计算基数天数)
    """
    actual_days = calculate_days(start_date, end_date)

    if day_count_convention == DayCountConvention.ACTUAL_360:
        return actual_days, 360
    elif day_count_convention == DayCountConvention.ACTUAL_365:
        return actual_days, 365
    else:
        # actual/actual：计算实际年份的天数
        start = parse_date(start_date)
        end = parse_date(end_date)

        # 如果在同一年
        if start.year == end.year:
            year_days = 366 if is_leap_year(start.year) else 365
            return actual_days, year_days
        else:
            # 跨年情况，返回实际天数和360（简化处理）
            return actual_days, 360


def is_leap_year(year: int) -> bool:
    """
    判断是否为闰年

    Args:
        year: 年份

    Returns:
        bool: 是否为闰年
    """
    if year % 4 != 0:
        return False
    elif year % 100 != 0:
        return True
    else:
        return year % 400 == 0


def add_years(
    base_date: Union[str, date, datetime],
    years: int
) -> date:
    """
    在基准日期上增加年份

    Args:
        base_date: 基准日期
        years: 增加的年数（可为负数）

    Returns:
        datetime.date: 计算后的日期
    """
    base = parse_date(base_date)
    return base + relativedelta(years=years)


def add_months(
    base_date: Union[str, date, datetime],
    months: int
) -> date:
    """
    在基准日期上增加月份

    Args:
        base_date: 基准日期
        months: 增加的月数（可为负数）

    Returns:
        datetime.date: 计算后的日期
    """
    base = parse_date(base_date)
    return base + relativedelta(months=months)


def add_days(
    base_date: Union[str, date, datetime],
    days: int
) -> date:
    """
    在基准日期上增加天数

    Args:
        base_date: 基准日期
        days: 增加的天数（可为负数）

    Returns:
        datetime.date: 计算后的日期
    """
    base = parse_date(base_date)
    return base + timedelta(days=days)


def get_date_range(
    start_date: Union[str, date, datetime],
    end_date: Union[str, date, datetime],
    freq: str = "D"
) -> List[date]:
    """
    生成日期范围

    Args:
        start_date: 起始日期
        end_date: 截止日期
        freq: 频率
            - 'D': 每日
            - 'W': 每周
            - 'M': 每月
            - 'Q': 每季度
            - 'Y': 每年

    Returns:
        List[datetime.date]: 日期列表
    """
    start = parse_date(start_date)
    end = parse_date(end_date)

    dates = []
    current = start

    if freq == 'D':
        while current <= end:
            dates.append(current)
            current = add_days(current, 1)
    elif freq == 'W':
        while current <= end:
            dates.append(current)
            current = add_days(current, 7)
    elif freq == 'M':
        while current <= end:
            dates.append(current)
            current = add_months(current, 1)
    elif freq == 'Q':
        while current <= end:
            dates.append(current)
            current = add_months(current, 3)
    elif freq == 'Y':
        while current <= end:
            dates.append(current)
            current = add_years(current, 1)
    else:
        raise ValueError(f"不支持的频率: {freq}")

    return dates


def validate_date_order(
    start_date: Union[str, date, datetime],
    end_date: Union[str, date, datetime],
    field_name: str = "日期"
) -> bool:
    """
    验证日期顺序是否正确

    Args:
        start_date: 起始日期
        end_date: 截止日期
        field_name: 字段名称（用于错误提示）

    Returns:
        bool: 日期顺序是否正确

    Raises:
        ValueError: 日期顺序错误
    """
    start = parse_date(start_date)
    end = parse_date(end_date)

    if start > end:
        raise ValueError(
            f"{field_name}日期顺序错误：起始日期({format_date(start)}) "
            f"不能晚于截止日期({format_date(end)})"
        )

    return True


def get_current_date() -> date:
    """
    获取当前日期（不包含时间部分）

    Returns:
        datetime.date: 当前日期
    """
    return datetime.now().date()


def date_diff_in_days(
    date1: Union[str, date, datetime],
    date2: Union[str, date, datetime]
) -> int:
    """
    计算两个日期的绝对天数差

    Args:
        date1: 日期1
        date2: 日期2

    Returns:
        int: 绝对天数差
    """
    d1 = parse_date(date1)
    d2 = parse_date(date2)
    return abs((d2 - d1).days)


def is_same_month(date1: Union[str, date, datetime], date2: Union[str, date, datetime]) -> bool:
    """
    判断两个日期是否在同一月

    Args:
        date1: 日期1
        date2: 日期2

    Returns:
        bool: 是否在同一月
    """
    d1 = parse_date(date1)
    d2 = parse_date(date2)
    return d1.year == d2.year and d1.month == d2.month


def get_last_day_of_month(date_input: Union[str, date, datetime]) -> date:
    """
    获取日期所在月份的最后一天

    Args:
        date_input: 输入日期

    Returns:
        datetime.date: 当月最后一天
    """
    d = parse_date(date_input)
    # 下个月的第一天减一天
    next_month = add_months(d, 1)
    first_day_next_month = date(next_month.year, next_month.month, 1)
    return add_days(first_day_next_month, -1)


if __name__ == "__main__":
    # 测试代码
    print("日期工具测试")
    print("=" * 50)

    # 测试日期解析
    test_date = "2024-01-15"
    parsed = parse_date(test_date)
    print(f"解析日期: {test_date} -> {parsed}")

    # 测试天数计算
    start = "2024-01-01"
    end = "2024-01-31"
    days = calculate_days(start, end)
    print(f"天数计算: {start} 到 {end} = {days}天")

    # 测试利息天数
    actual, base = calculate_interest_days(start, end)
    print(f"利息天数: 实际{actual}天 / 基数{base}天")

    # 测试日期验证
    try:
        validate_date_order("2024-01-15", "2024-01-01")
    except ValueError as e:
        print(f"日期验证错误: {e}")
