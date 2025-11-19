-- 更新分类图标为 iconType:iconName 格式
-- 此迁移脚本将系统预设分类的 emoji 图标替换为 vector icon 格式

-- 更新支出分类图标
UPDATE category SET icon = 'ionicons:restaurant' WHERE name = '餐饮' AND is_system = true;
UPDATE category SET icon = 'ionicons:cart' WHERE name = '购物' AND is_system = true;
UPDATE category SET icon = 'ionicons:car' WHERE name = '交通' AND is_system = true;
UPDATE category SET icon = 'ionicons:home' WHERE name = '日用' AND is_system = true;
UPDATE category SET icon = 'ionicons:game-controller' WHERE name = '娱乐' AND is_system = true;
UPDATE category SET icon = 'ionicons:medical' WHERE name = '医疗' AND is_system = true;
UPDATE category SET icon = 'ionicons:book' WHERE name = '教育' AND is_system = true;
UPDATE category SET icon = 'ionicons:phone-portrait' WHERE name = '通讯' AND is_system = true;

-- 更新收入分类图标
UPDATE category SET icon = 'ionicons:wallet' WHERE name = '工资' AND is_system = true;
UPDATE category SET icon = 'ionicons:gift' WHERE name = '奖金' AND is_system = true;
UPDATE category SET icon = 'ionicons:trending-up' WHERE name = '理财' AND is_system = true;
UPDATE category SET icon = 'ionicons:briefcase' WHERE name = '兼职' AND is_system = true;
