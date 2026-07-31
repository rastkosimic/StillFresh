# Build from repo root so legal markdown files are available:
#   docker build -f marketing-web/Dockerfile -t stillfresh-marketing .
FROM node:20-alpine AS build
WORKDIR /app

COPY marketing-web/package.json marketing-web/package-lock.json ./marketing-web/
WORKDIR /app/marketing-web
RUN npm ci

COPY marketing-web/ ./
COPY PRIVACY.md TERMS_CUSTOMER.md TERMS_VENDOR.md ../

RUN npm run build

FROM nginx:alpine
COPY marketing-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/marketing-web/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
