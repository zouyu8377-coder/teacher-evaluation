# =====================================================
# 运行阶段：Nginx静态文件服务（宿主机预构建dist直接打包）
# 前置要求：先在宿主机执行 npm run build
# =====================================================
FROM nginx:alpine

# 复制宿主机已构建的静态文件
COPY dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
