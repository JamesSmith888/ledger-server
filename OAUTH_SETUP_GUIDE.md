# OAuth 第三方登录配置指南

## 📋 支付宝配置步骤

### 1. 注册开放平台账号
访问 [支付宝开放平台](https://open.alipay.com/)，注册开发者账号。

### 2. 创建应用
1. 登录后，进入「开发者中心」
2. 选择「网页/移动应用」
3. 创建应用，填写应用信息
4. 等待审核通过

### 3. 配置应用
审核通过后：
1. 进入应用详情
2. 添加功能：「获取会员信息」（alipay.user.info.share）
3. 配置「授权回调地址」（移动应用可填写 app scheme）

### 4. 生成密钥
支付宝使用 RSA2 签名，需要生成密钥对：

#### 方式1: 使用支付宝密钥生成工具
1. 下载 [支付宝密钥生成工具](https://opendocs.alipay.com/common/02kipl)
2. 运行工具，选择「RSA2(SHA256)」
3. 生成后会得到：
   - **应用私钥**（保密，配置到你的服务器）
   - **应用公钥**（上传到支付宝开放平台）

#### 方式2: 使用 OpenSSL 命令行
```bash
# 生成私钥
openssl genrsa -out app_private_key.pem 2048

# 从私钥中提取公钥
openssl rsa -in app_private_key.pem -pubout -out app_public_key.pem

# 转换为 PKCS8 格式（Java需要）
openssl pkcs8 -topk8 -inform PEM -in app_private_key.pem -outform PEM -nocrypt -out app_private_key_pkcs8.pem
```

### 5. 配置密钥到支付宝平台
1. 在应用详情中，找到「接口加签方式」
2. 选择「公钥」模式
3. 上传你生成的**应用公钥**
4. 保存后，支付宝会生成**支付宝公钥**（用于验证支付宝返回的数据）

### 6. 获取配置信息
完成以上步骤后，你会得到：
- **App ID**: 应用的唯一标识（如：2021001234567890）
- **应用私钥**: 你自己生成的私钥（PKCS8格式，去掉头尾）
- **支付宝公钥**: 支付宝平台生成的公钥（用于验证签名）

### 7. 配置到项目
编辑 `application.yml`：

```yaml
oauth:
  alipay:
    app-id: 2021001234567890
    private-key: MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...
    alipay-public-key: MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
```

**注意**：
- `private-key`: 去掉 `-----BEGIN PRIVATE KEY-----` 和 `-----END PRIVATE KEY-----`，只保留中间的字符串
- `alipay-public-key`: 去掉 `-----BEGIN PUBLIC KEY-----` 和 `-----END PUBLIC KEY-----`，只保留中间的字符串
- 生产环境建议通过**环境变量**配置，不要提交到 Git

### 8. 环境变量配置（推荐）
```bash
export ALIPAY_APP_ID=2021001234567890
export ALIPAY_PRIVATE_KEY=MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...
export ALIPAY_PUBLIC_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
```

---

## 🔧 测试支付宝登录

### 前端测试（React Native）
```typescript
import Alipay from '@uiw/react-native-alipay';

const loginWithAlipay = async () => {
  try {
    // 构造授权信息串
    const authInfo = `apiname=com.alipay.account.auth&app_id=${APP_ID}&app_name=mc&auth_type=AUTHACCOUNT&biz_type=openservice&pid=${PID}&product_id=APP_FAST_LOGIN&scope=kuaijie&sign_type=RSA2`;
    
    // 调用支付宝 SDK
    const result = await Alipay.authWithInfo(authInfo);
    
    if (result.resultStatus === '9000') {
      // 解析 auth_code
      const authCode = parseAuthCode(result.result);
      
      // 发送到后端
      const response = await fetch('http://your-server/oauth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          oauthType: 'ALIPAY',
          code: authCode
        })
      });
      
      const data = await response.json();
      console.log('登录成功:', data);
    }
  } catch (error) {
    console.error('支付宝登录失败:', error);
  }
};

function parseAuthCode(resultStr) {
  const match = resultStr.match(/auth_code=([^&]+)/);
  return match ? match[1] : null;
}
```

### 后端测试（cURL）
```bash
curl -X POST http://localhost:9432/oauth/login \
  -H "Content-Type: application/json" \
  -d '{
    "oauthType": "ALIPAY",
    "code": "你从前端获取的auth_code"
  }'
```

---

## 🌟 使用的 JDK 21+ 特性

本实现充分利用了现代 Java 特性：

### 1. Record（记录类）
```java
// 简洁的不可变数据类
public record OAuthUserInfo(
    String oauthId,
    OAuthType oauthType,
    String nickname,
    String avatarUrl,
    ...
) {}
```

### 2. Pattern Matching for Switch
```java
// 类型安全的 switch 表达式
String credential = switch (oauthType) {
    case WECHAT, ALIPAY, APPLE -> code;
    case GOOGLE -> idToken;
};
```

### 3. Sealed Classes（可扩展）
```java
// 限制子类，确保类型安全
public sealed interface OAuthService 
    permits WeChatOAuthService, AlipayOAuthService, ... {}
```

### 4. Text Blocks（多行字符串）
```java
String sql = """
    SELECT * FROM user_oauth
    WHERE oauth_type = ? AND oauth_id = ?
    """;
```

### 5. Stream API 增强
```java
// 函数式编程风格
Map<OAuthType, OAuthService> map = services.stream()
    .collect(Collectors.toMap(
        OAuthService::getOAuthType,
        Function.identity()
    ));
```

---

## 📚 API 接口文档

### 1. 第三方登录
```http
POST /oauth/login
Content-Type: application/json

{
  "oauthType": "ALIPAY",
  "code": "ca5e3e2e..."
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresAt": "2025-11-26T12:00:00",
    "userId": 123,
    "username": "ali_12345678",
    "nickname": "支付宝用户",
    "avatarUrl": "https://..."
  }
}
```

### 2. 绑定第三方账号
```http
POST /oauth/bind
Authorization: Bearer <your-jwt-token>
Content-Type: application/json

{
  "oauthType": "ALIPAY",
  "code": "ca5e3e2e..."
}
```

### 3. 解绑第三方账号
```http
DELETE /oauth/unbind/alipay
Authorization: Bearer <your-jwt-token>
```

---

## 🚀 扩展其他平台

框架已支持扩展，添加微信/Google/Apple只需：

1. 实现 `OAuthService` 接口
2. 添加 `@Service` 注解
3. 配置对应的 `application.yml`

**示例**（微信）：
```java
@Service
public class WeChatOAuthServiceImpl implements OAuthService {
    @Override
    public OAuthUserInfo getUserInfo(String code) {
        // 实现微信登录逻辑
    }
    
    @Override
    public OAuthType getOAuthType() {
        return OAuthType.WECHAT;
    }
}
```

框架会自动注册并使用！🎉
