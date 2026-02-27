#!/bin/bash

# Swagger Verification Script for All Services
# Tests Swagger UI and OpenAPI endpoints

# Colors
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
WHITE='\033[1;37m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

echo -e "${CYAN}"
echo "========================================"
echo "  Swagger UI Verification Script"
echo "  Food Ordering Microservices"
echo "========================================"
echo -e "${NC}"

# Define services
declare -A auth_service=(
    [name]="Auth Service"
    [port]=8081
    [swagger]="http://localhost:8081/swagger-ui.html"
    [docs]="http://localhost:8081/v3/api-docs"
    [health]="http://localhost:8081/auth/health"
)

declare -A api_gateway=(
    [name]="API Gateway"
    [port]=8080
    [swagger]="http://localhost:8080/swagger-ui.html"
    [docs]="http://localhost:8080/v3/api-docs"
    [health]="http://localhost:8080/health"
)

all_passed=true

test_service() {
    local -n service=$1
    
    echo -e "\n${WHITE}┌────────────────────────────────────────┐${NC}"
    echo -e "${WHITE}│  Testing: ${service[name]}                │${NC}"
    echo -e "${WHITE}└────────────────────────────────────────┘${NC}"
    
    # Test 1: Service Health Check
    echo -e "\n${YELLOW}1️⃣  Health Check...${NC}"
    health_status=$(curl -s -o /dev/null -w "%{http_code}" "${service[health]}" 2>/dev/null)
    
    if [ "$health_status" == "200" ]; then
        echo -e "   ${GREEN}✅ Service is running${NC}"
        health_response=$(curl -s "${service[health]}")
        echo -e "   ${GRAY}Status: UP${NC}"
    else
        echo -e "   ${RED}❌ Service not responding${NC}"
        echo -e "   ${GRAY}HTTP Status: $health_status${NC}"
        echo -e "   ${YELLOW}Make sure the service is running on port ${service[port]}${NC}"
        all_passed=false
        return
    fi
    
    # Test 2: OpenAPI JSON Docs
    echo -e "\n${YELLOW}2️⃣  OpenAPI JSON Docs...${NC}"
    docs_status=$(curl -s -o /dev/null -w "%{http_code}" "${service[docs]}" 2>/dev/null)
    
    if [ "$docs_status" == "200" ]; then
        echo -e "   ${GREEN}✅ OpenAPI docs accessible${NC}"
        echo -e "   ${GRAY}URL: ${service[docs]}${NC}"
        
        # Get OpenAPI version and path count
        docs_content=$(curl -s "${service[docs]}")
        openapi_version=$(echo "$docs_content" | grep -o '"openapi":"[^"]*"' | cut -d'"' -f4)
        path_count=$(echo "$docs_content" | grep -o '"\/[^"]*":' | wc -l)
        
        if [ ! -z "$openapi_version" ]; then
            echo -e "   ${GRAY}OpenAPI Version: $openapi_version${NC}"
        fi
        
        if [ "$path_count" -gt 0 ]; then
            echo -e "   ${GRAY}Documented Paths: $path_count${NC}"
        fi
    else
        echo -e "   ${RED}❌ OpenAPI docs not accessible${NC}"
        echo -e "   ${GRAY}URL: ${service[docs]}${NC}"
        all_passed=false
    fi
    
    # Test 3: Swagger UI
    echo -e "\n${YELLOW}3️⃣  Swagger UI...${NC}"
    swagger_status=$(curl -s -o /dev/null -w "%{http_code}" "${service[swagger]}" 2>/dev/null)
    
    if [ "$swagger_status" == "200" ]; then
        echo -e "   ${GREEN}✅ Swagger UI accessible${NC}"
        echo -e "   ${GRAY}URL: ${service[swagger]}${NC}"
        
        # Check if response contains swagger-ui
        swagger_content=$(curl -s "${service[swagger]}")
        if echo "$swagger_content" | grep -q "swagger-ui"; then
            echo -e "   ${GREEN}✅ Swagger UI content verified${NC}"
        else
            echo -e "   ${YELLOW}⚠️  Response received but may not be Swagger UI${NC}"
        fi
    else
        echo -e "   ${RED}❌ Swagger UI not accessible${NC}"
        echo -e "   ${GRAY}URL: ${service[swagger]}${NC}"
        all_passed=false
    fi
    
    # Test 4: Browser Instructions
    echo -e "\n${YELLOW}4️⃣  Browser Access...${NC}"
    echo -e "   ${CYAN}Open in browser: ${service[swagger]}${NC}"
    echo -e "   ${GRAY}Or run: open ${service[swagger]} (Mac) / xdg-open ${service[swagger]} (Linux)${NC}"
}

# Test all services
test_service auth_service
test_service api_gateway

# Summary
echo -e "\n${CYAN}"
echo "========================================"
echo "  Test Summary"
echo "========================================"
echo -e "${NC}"

if [ "$all_passed" = true ]; then
    echo -e "${GREEN}🎉 All services passed Swagger verification!${NC}"
    echo ""
    echo -e "${CYAN}📌 Quick Access:${NC}"
    echo -e "   ${WHITE}Auth Service:  http://localhost:8081/swagger-ui.html${NC}"
    echo -e "   ${WHITE}API Gateway:   http://localhost:8080/swagger-ui.html${NC}"
    echo ""
    echo -e "${YELLOW}💡 Next Steps:${NC}"
    echo -e "   ${GRAY}1. Open Swagger UI in your browser${NC}"
    echo -e "   ${GRAY}2. Try the 'POST /auth/register' endpoint${NC}"
    echo -e "   ${GRAY}3. Login with 'POST /auth/login' to get JWT token${NC}"
    echo -e "   ${GRAY}4. Click 'Authorize' and enter: Bearer <your-token>${NC}"
    echo -e "   ${GRAY}5. Test protected endpoints${NC}"
else
    echo -e "${YELLOW}⚠️  Some services failed verification${NC}"
    echo ""
    echo -e "${YELLOW}🔧 Troubleshooting:${NC}"
    echo -e "   ${GRAY}1. Check if services are running:${NC}"
    echo -e "      ${GRAY}cd auth-service && mvn spring-boot:run${NC}"
    echo -e "      ${GRAY}cd api-gateway && mvn spring-boot:run${NC}"
    echo ""
    echo -e "   ${GRAY}2. Verify port availability:${NC}"
    echo -e "      ${GRAY}lsof -i :8081${NC}"
    echo -e "      ${GRAY}lsof -i :8080${NC}"
    echo ""
    echo -e "   ${GRAY}3. Check logs for errors${NC}"
    echo ""
    echo -e "   ${GRAY}4. Rebuild services:${NC}"
    echo -e "      ${GRAY}mvn clean package -DskipTests${NC}"
fi

echo ""
echo -e "${CYAN}📚 Documentation: SWAGGER_API_DOCUMENTATION.md${NC}"
echo ""
