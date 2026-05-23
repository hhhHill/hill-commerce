import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "hill-commerce.oss-cn-shenzhen.aliyuncs.com"
      }
    ]
  }
};

export default nextConfig;
