import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  output: 'export',
  trailingSlash: false,
  images: { unoptimized: true },
  env: { NEXT_PUBLIC_E2E_TEST_IDENTITY: process.env.NEXT_PUBLIC_E2E_TEST_IDENTITY ?? 'off' },
};

export default nextConfig;
