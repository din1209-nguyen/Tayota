import ConciergeTools from "@/components/home/ConciergeTools";
import ExperienceBand from "@/components/home/ExperienceBand";
import FeaturedArticles from "@/components/home/FeaturedArticles";
import FeaturedVehicles from "@/components/home/FeaturedVehicles";
import HeroSection from "@/components/home/HeroSection";
import OwnershipServices from "@/components/home/OwnershipServices";

export default function HomePage() {
  return (
    <>
      <HeroSection />
      <FeaturedVehicles />
      <ExperienceBand />
      <ConciergeTools />
      <OwnershipServices />
      <FeaturedArticles />
    </>
  );
}
