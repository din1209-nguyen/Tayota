import { Geist, Geist_Mono } from "next/font/google";
import ChatLauncher from "@/components/chat/ChatLauncher";
import Footer from "@/components/layout/Footer";
import Header from "@/components/layout/Header";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata = {
  title: "Tayota",
  description: "Tayota - showroom xe sang, tư vấn mua xe, lái thử riêng và dịch vụ sở hữu cao cấp.",
};

export default function RootLayout({ children }) {
  return (
    <html lang="vi" suppressHydrationWarning className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col">
        <Header />
        <main className="site-main">{children}</main>
        <ChatLauncher />
        <Footer />
      </body>
    </html>
  );
}
