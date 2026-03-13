import HeroSection from "@/components/sections/HeroSection";
import FeatureGrid from "@/components/sections/FeatureGrid";

export default function HomePage() {
	return (
		<div className='space-y-6'>
			<HeroSection />
			<FeatureGrid />
		</div>
	);
}
