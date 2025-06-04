FROM node:22-alpine
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . ./
CMD npm run build
CMD npm run dev -- --host 0.0.0.0
EXPOSE 5173