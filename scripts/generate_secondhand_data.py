#!/usr/bin/env python3
"""
二手交易测试数据生成脚本
生成 100 条真实的二手商品数据并上传图片到阿里云 OSS

安装依赖:
pip install oss2 pymysql requests faker

使用前请填写 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET
"""

import os
import random
import time
import json
import requests
import pymysql
import oss2
from datetime import datetime
from faker import Faker

# ============== 配置区域 ==============
# OSS 配置 (从环境变量读取)
OSS_ACCESS_KEY_ID = os.environ.get('OSS_ACCESS_KEY_ID', '')
OSS_ACCESS_KEY_SECRET = os.environ.get('OSS_ACCESS_KEY_SECRET', '')
OSS_ENDPOINT = 'https://oss-cn-beijing.aliyuncs.com'
OSS_BUCKET = 'caspar-java-tlias-pratice'
OSS_REGION = 'cn-beijing'

# 数据库配置
DB_HOST = 'localhost'
DB_PORT = 3306
DB_USER = 'root'
DB_PASSWORD = '12345678'
DB_NAME = 'campus_platform'

# 发布用户 ID
USER_IDS = [2, 4]  # testuser123 和 user

# 生成数量
GENERATE_COUNT = 100
# =====================================

fake = Faker('zh_CN')

# 商品分类
CATEGORIES = ['教材书籍', '数码产品', '体育用品', '生活杂物', '寝室神器']

# 商品条件
CONDITIONS = ['全新', '九成新', '八成新', '七成新']

# 商品标题模板
TITLE_TEMPLATES = {
    '教材书籍': [
        '九成新《{}》教材', '转让《{}》课本', '科大{}课程教材',
        '{}专业书籍转让', '正版《{}》', '{}相关书籍',
        '《{}》几乎全新', '{}学习资料', '考研{}参考书', '{}考试用书'
    ],
    '数码产品': [
        '转让{}自用', '九成新{}', '科大西区{}转让',
        '{}闲置出售', '自用{}低价出', '{}功能完好',
        '{}急出', '{}无划痕', '{}带配件', '{}保修中'
    ],
    '体育用品': [
        '转让{}九成新', '{}闲置出', '自用{}',
        '{}几乎没用过', '{}适合新手', '{}专业级',
        '{}带包', '{}尺码可调', '{}适合科大操场', '{}运动装备'
    ],
    '生活杂物': [
        '转让{}闲置', '{}低价出', '{}九成新',
        '寝室{}转让', '{}实用款', '{}几乎全新',
        '{}搬家急出', '{}自用转让', '{}质量好', '{}便宜出'
    ],
    '寝室神器': [
        '{}寝室必备', '{}宿舍神器', '{}节省空间',
        '{}适合寝室', '{}小户型适用', '{}收纳神器',
        '{}实用转让', '{}九成新', '{}宿舍好物', '{}生活必备'
    ]
}

# 商品名称池
ITEM_NAMES = {
    '教材书籍': [
        '算法导论', '数据结构', '操作系统', '计算机网络', '高等数学',
        '线性代数', '概率论', '大学物理', '离散数学', '编译原理',
        '计算机组成原理', '数据库系统概念', '机器学习', '深度学习', 'Python编程',
        'Java核心技术', 'C++ Primer', '设计模式', '软件工程', '人工智能'
    ],
    '数码产品': [
        '小米耳机', '华为充电器', 'iPad保护壳', '机械键盘', '蓝牙音箱',
        '移动硬盘', 'U盘', '鼠标', '显示器支架', '手机支架',
        '充电宝', '数据线', '摄像头', '麦克风', '台灯',
        '路由器', '网线', '转接头', '读卡器', '手写板'
    ],
    '体育用品': [
        '羽毛球拍', '篮球', '足球', '乒乓球拍', '网球拍',
        '瑜伽垫', '哑铃', '跳绳', '护膝', '运动水壶',
        '跑步腰包', '运动手环', '泳镜', '泳帽', '运动毛巾',
        '护腕', '运动背包', '羽毛球', '乒乓球', '排球'
    ],
    '生活杂物': [
        '插排', '小风扇', '收纳盒', '衣架', '拖鞋',
        '水杯', '雨伞', '镜子', '挂钩', '置物架',
        '垃圾桶', '扫把', '拖把', '洗衣液', '纸巾盒',
        '闹钟', '温度计', '体重秤', '针线包', '工具箱'
    ],
    '寝室神器': [
        '床上书桌', '遮光帘', '收纳挂袋', '床头置物架', '折叠椅',
        '小电锅', '迷你冰箱', '加湿器', '除湿袋', '蚊帐',
        '床边挂篮', '插座收纳盒', '衣柜分隔板', '鞋架', '晾衣绳',
        '门后挂钩', '桌面收纳架', '化妆品收纳盒', '书立', '抽屉分隔盒'
    ]
}

# 描述模板
DESC_TEMPLATES = [
    '因毕业/搬家/换新，转让自用{item}，{condition}，功能完好无损坏。{extra}',
    '闲置转让{item}，{condition}，价格可小刀。{extra}',
    '自用{item}转让，{condition}，诚心出的来。{extra}',
    '{item}闲置出，{condition}，不议价谢谢。{extra}',
    '转让{item}，{condition}，科大西区可面交。{extra}',
    '{item}低价出，{condition}，先到先得。{extra}',
    '毕业清仓{item}，{condition}，数量有限。{extra}',
    '{item}急出，{condition}，有意私聊。{extra}',
    '自用{item}转让，{condition}，非诚勿扰。{extra}',
    '{item}闲置，{condition}，可小刀。{extra}'
]

EXTRA_INFO = [
    '有意者私聊详询', '可面交验货', '支持校内交易',
    '不包邮', '顺丰到付', '西区宿舍可自提',
    '诚心出的来', '价格可谈', '急出可优惠',
    '质量保证', '无任何问题', '功能正常'
]

# Unsplash 图片 URL 模板 (使用 picsum.photos 作为备用)
IMAGE_SOURCES = [
    'https://picsum.photos/seed/{}/800/600',
    'https://picsum.photos/seed/{}/600/800',
    'https://picsum.photos/seed/{}/700/700',
]


def get_oss_bucket():
    """获取 OSS Bucket 实例"""
    auth = oss2.Auth(OSS_ACCESS_KEY_ID, OSS_ACCESS_KEY_SECRET)
    bucket = oss2.Bucket(auth, OSS_ENDPOINT, OSS_BUCKET)
    return bucket


def upload_image_to_oss(bucket, image_url, object_name):
    """下载图片并上传到 OSS"""
    try:
        # 下载图片
        response = requests.get(image_url, timeout=10)
        if response.status_code == 200:
            # 上传到 OSS
            bucket.put_object(object_name, response.content)
            # 返回 OSS URL
            return f'https://{OSS_BUCKET}.oss-{OSS_REGION}.aliyuncs.com/{object_name}'
    except Exception as e:
        print(f'上传图片失败: {e}')
    return None


def generate_random_images(bucket, item_id, category):
    """生成随机图片 URL"""
    # 根据分类选择不同的图片种子
    category_seeds = {
        '教材书籍': ['book', 'textbook', 'study', 'education', 'library'],
        '数码产品': ['tech', 'computer', 'phone', 'electronic', 'device'],
        '体育用品': ['sport', 'fitness', 'ball', 'exercise', 'gym'],
        '生活杂物': ['home', 'life', 'daily', 'household', 'stuff'],
        '寝室神器': ['room', 'dorm', 'storage', 'organize', 'bed']
    }
    
    seeds = category_seeds.get(category, ['random'])
    image_count = random.randint(1, 3)
    images = []
    
    for i in range(image_count):
        seed = random.choice(seeds) + str(item_id) + str(i) + str(random.randint(1, 1000))
        image_url = random.choice(IMAGE_SOURCES).format(seed)
        
        # 生成 OSS 对象名
        timestamp = int(time.time() * 1000)
        object_name = f'secondhand/{timestamp}_{item_id}_{i}.jpg'
        
        # 上传图片
        oss_url = upload_image_to_oss(bucket, image_url, object_name)
        if oss_url:
            images.append(oss_url)
        
        # 避免请求过快
        time.sleep(0.1)
    
    return ','.join(images) if images else None


def generate_title(category):
    """生成随机标题"""
    template = random.choice(TITLE_TEMPLATES[category])
    item_name = random.choice(ITEM_NAMES[category])
    return template.format(item_name)


def generate_description(title, category):
    """生成随机描述"""
    template = random.choice(DESC_TEMPLATES)
    condition = random.choice(CONDITIONS)
    extra = random.choice(EXTRA_INFO)
    return template.format(item=title, condition=condition, extra=extra)


def generate_price(category):
    """根据分类生成合理价格"""
    price_ranges = {
        '教材书籍': (10, 100),
        '数码产品': (50, 2000),
        '体育用品': (20, 500),
        '生活杂物': (5, 200),
        '寝室神器': (15, 300)
    }
    min_price, max_price = price_ranges.get(category, (10, 500))
    return round(random.uniform(min_price, max_price), 2)


def insert_to_database(conn, data):
    """插入数据到数据库"""
    cursor = conn.cursor()
    sql = """
        INSERT INTO secondhand 
        (title, description, price, category, images, `condition`, status, seller_id, view_count, create_time)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    cursor.execute(sql, (
        data['title'],
        data['description'],
        data['price'],
        data['category'],
        data['images'],
        data['condition'],
        data['status'],
        data['seller_id'],
        data['view_count'],
        data['create_time']
    ))
    conn.commit()
    return cursor.lastrowid


def main():
    # 检查 OSS 配置
    if not OSS_ACCESS_KEY_ID or not OSS_ACCESS_KEY_SECRET:
        print('错误: 请先填写 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET')
        print('请在脚本顶部的配置区域填写你的阿里云 OSS 凭证')
        return
    
    print('=' * 50)
    print('二手交易测试数据生成脚本')
    print('=' * 50)
    print(f'将生成 {GENERATE_COUNT} 条测试数据')
    print(f'发布用户: {USER_IDS}')
    print(f'OSS Bucket: {OSS_BUCKET}')
    print('=' * 50)
    
    # 连接数据库
    print('\n正在连接数据库...')
    conn = pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        charset='utf8mb4'
    )
    print('数据库连接成功!')
    
    # 获取 OSS Bucket
    print('\n正在初始化 OSS...')
    bucket = get_oss_bucket()
    print('OSS 初始化成功!')
    
    # 生成数据
    print(f'\n开始生成 {GENERATE_COUNT} 条数据...\n')
    
    success_count = 0
    for i in range(GENERATE_COUNT):
        try:
            # 随机选择分类
            category = random.choice(CATEGORIES)
            
            # 生成数据
            title = generate_title(category)
            description = generate_description(title, category)
            price = generate_price(category)
            condition = random.choice(CONDITIONS)
            seller_id = random.choice(USER_IDS)
            view_count = random.randint(0, 500)
            
            # 随机创建时间 (最近30天内)
            days_ago = random.randint(0, 30)
            hours_ago = random.randint(0, 23)
            create_time = datetime.now().replace(
                hour=random.randint(0, 23),
                minute=random.randint(0, 59),
                second=random.randint(0, 59)
            ) - timedelta(days=days_ago, hours=hours_ago)
            
            data = {
                'title': title,
                'description': description,
                'price': price,
                'category': category,
                'images': None,  # 先插入数据获取 ID
                'condition': condition,
                'status': 'ACTIVE',
                'seller_id': seller_id,
                'view_count': view_count,
                'create_time': create_time
            }
            
            # 插入数据库
            item_id = insert_to_database(conn, data)
            
            # 生成并上传图片
            images = generate_random_images(bucket, item_id, category)
            
            # 更新图片字段
            if images:
                cursor = conn.cursor()
                cursor.execute('UPDATE secondhand SET images = %s WHERE id = %s', (images, item_id))
                conn.commit()
            
            success_count += 1
            print(f'[{success_count}/{GENERATE_COUNT}] 已生成: {title} - ¥{price}')
            
        except Exception as e:
            print(f'生成第 {i+1} 条数据失败: {e}')
    
    # 关闭数据库连接
    conn.close()
    
    print('\n' + '=' * 50)
    print(f'数据生成完成! 成功: {success_count}/{GENERATE_COUNT}')
    print('=' * 50)


if __name__ == '__main__':
    from datetime import timedelta
    main()
