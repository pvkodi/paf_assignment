FROM node:20-alpine AS build
WORKDIR /app
ARG VITE_API_BASE_URL
COPY package*.json ./
RUN npm install
COPY . .
# If a local .env isn't present, create one from the build arg so Vite picks it up
RUN if [ ! -f .env ] && [ -n "$VITE_API_BASE_URL" ]; then printf 'VITE_API_BASE_URL=%s\n' "$VITE_API_BASE_URL" > .env; fi
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
