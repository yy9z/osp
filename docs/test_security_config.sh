#!/bin/bash

# Spring Security 配置验证脚本
# 用于测试权限规则是否正确配置
# 使用方法：bash test_security_config.sh

set -e

# 配置
BASE_URL="http://localhost:8080"
USERNAME="testuser"
PASSWORD="123456"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 计数器
PASSED=0
FAILED=0

# 帮助函数
print_header() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
}

print_success() {
    echo -e "${GREEN}✓ PASS: $1${NC}"
    ((PASSED++))
}

print_failure() {
    echo -e "${RED}✗ FAIL: $1${NC}"
    ((FAILED++))
}

print_info() {
    echo -e "${YELLOW}ℹ INFO: $1${NC}"
}

# 测试函数：期望 200
test_expect_200() {
    local method=$1
    local endpoint=$2
    local description=$3
    
    print_info "Testing: $method $endpoint"
    
    response_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X "$method" \
        "$BASE_URL$endpoint")
    
    if [ "$response_code" = "200" ]; then
        print_success "$description (HTTP $response_code)"
    else
        print_failure "$description (HTTP $response_code, expected 200)"
    fi
}

# 测试函数：期望 401
test_expect_401() {
    local method=$1
    local endpoint=$2
    local description=$3
    
    print_info "Testing: $method $endpoint"
    
    response_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X "$method" \
        "$BASE_URL$endpoint")
    
    if [ "$response_code" = "401" ] || [ "$response_code" = "403" ]; then
        print_success "$description (HTTP $response_code)"
    else
        print_failure "$description (HTTP $response_code, expected 401/403)"
    fi
}

# 测试函数：期望 CORS 响应头
test_cors_headers() {
    local endpoint=$1
    local origin=$2
    
    print_info "Testing CORS for: $endpoint"
    
    response=$(curl -s -i -X OPTIONS "$BASE_URL$endpoint" \
        -H "Origin: $origin" \
        -H "Access-Control-Request-Method: GET" \
        -H "Access-Control-Request-Headers: Content-Type,Authorization" 2>&1)
    
    if echo "$response" | grep -q "Access-Control-Allow-Origin"; then
        print_success "CORS headers present for $endpoint"
    else
        print_failure "CORS headers missing for $endpoint"
    fi
}

# 获取 Token 的函数
get_token() {
    print_info "Attempting to login..."
    
    response=$(curl -s -X POST "$BASE_URL/api/user/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" 2>/dev/null)
    
    # 尝试从响应中提取 token（假设 JSON 格式）
    token=$(echo "$response" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    
    if [ -z "$token" ]; then
        print_failure "Failed to obtain token. Login response: $response"
        return 1
    fi
    
    print_success "Successfully obtained token"
    echo "$token"
}

# 测试带 Token 的请求
test_with_token() {
    local method=$1
    local endpoint=$2
    local token=$3
    local description=$4
    
    print_info "Testing: $method $endpoint (with token)"
    
    response_code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X "$method" \
        -H "Authorization: Bearer $token" \
        "$BASE_URL$endpoint")
    
    if [ "$response_code" = "200" ]; then
        print_success "$description (HTTP $response_code)"
    else
        print_failure "$description (HTTP $response_code, expected 200)"
    fi
}

# ============================================ 开始测试 ============================================

print_header "Spring Security Configuration Tests"

echo "Target Server: $BASE_URL"
echo ""

# 第 1 部分：前端资源测试
print_header "🎨 PART 1: Frontend Resources (Should be accessible without login)"

test_expect_200 "GET" "/" "Home page /"
test_expect_200 "GET" "/index.html" "HTML file /index.html"
test_expect_200 "GET" "/favicon.ico" "Favicon /favicon.ico"

echo ""
print_info "Note: /assets/** and /images/** tests require actual files to exist"
echo "      Following tests are conditional:"

if curl -s -f "$BASE_URL/assets/style.css" > /dev/null 2>&1; then
    test_expect_200 "GET" "/assets/style.css" "CSS file in /assets/**"
else
    print_info "Skipping /assets/style.css (file not found)"
fi

if curl -s -f "$BASE_URL/images/logo.png" > /dev/null 2>&1; then
    test_expect_200 "GET" "/images/logo.png" "Image in /images/**"
else
    print_info "Skipping /images/logo.png (file not found)"
fi

# 第 2 部分：公开 API 测试
print_header "📡 PART 2: Public APIs (Should be accessible without login)"

test_expect_200 "GET" "/api/secondhand/list" "Secondhand list API"
test_expect_200 "GET" "/api/lostfound/list" "Lost and Found list API"
test_expect_200 "GET" "/api/navigation/pois" "Navigation POIs API (example)"
test_expect_200 "GET" "/api/dormitory/list" "Dormitory list API"

# 第 3 部分：用户认证 API 测试
print_header "🔐 PART 3: Authentication APIs (Should be accessible without login)"

test_expect_200 "POST" "/api/user/register" "User registration endpoint"
test_expect_200 "POST" "/api/user/login" "User login endpoint"

# 第 4 部分：受保护的 API 测试（未认证）
print_header "🔒 PART 4: Protected APIs without Token (Should return 401)"

test_expect_401 "GET" "/api/user/profile" "User profile without token"

# 第 5 部分：受保护的 API 测试（已认证）
print_header "🔓 PART 5: Protected APIs with Token (Should return 200)"

print_info "Step 1: Obtaining authentication token..."
token=$(get_token)

if [ $? -eq 0 ] && [ ! -z "$token" ]; then
    print_info "Step 2: Testing protected endpoints with token..."
    sleep 1  # Brief pause
    
    test_with_token "GET" "/api/user/profile" "$token" "User profile with token"
    test_with_token "GET" "/api/user/dormitory" "$token" "Dormitory info with token"
fi

# 第 6 部分：CORS 测试
print_header "🌐 PART 6: CORS Configuration (Should have CORS headers)"

test_cors_headers "/" "http://localhost:3000"
test_cors_headers "/api/user/profile" "http://localhost:3000"
test_cors_headers "/api/secondhand/list" "http://frontend.example.com"

# 第 7 部分：API 文档测试
print_header "📚 PART 7: API Documentation (Should be publicly accessible)"

print_info "Testing: GET /swagger-ui.html"
response_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/swagger-ui.html")

if [ "$response_code" = "200" ] || [ "$response_code" = "302" ]; then
    print_success "Swagger UI is accessible (HTTP $response_code)"
else
    print_info "Swagger UI test skipped (HTTP $response_code - may not be configured)"
fi

# ============================================ 测试总结 ============================================

print_header "Test Summary"

total=$((PASSED + FAILED))
echo -e "Total Tests: $total"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed. Please check the configuration.${NC}"
    exit 1
fi
