import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",

  async rewrites() {
    return [
      {
        source: "/backend/:path*",
        destination: `${
          process.env.JAVA_API_URL ?? "http://localhost:8080"
        }/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
