import ConciergeTools from "@/components/home/ConciergeTools";
import FeaturedVehicles from "@/components/home/FeaturedVehicles";
import HeroSection from "@/components/home/HeroSection";
import OwnershipServices from "@/components/home/OwnershipServices";

export default function HomePage() {
  return (
    <>
      <HeroSection />
      <FeaturedVehicles />
      <ConciergeTools />
      <OwnershipServices />
    </>
  );
}
