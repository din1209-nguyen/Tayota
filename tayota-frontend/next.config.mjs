/** @type {import('next').NextConfig} */
const nextConfig = {
  /* config options here */
  reactCompiler: true,
  output: 'standalone' // gom những file thực sự cần thiết khi build
};

export default nextConfig;
