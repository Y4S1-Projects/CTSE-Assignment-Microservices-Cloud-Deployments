import HeroSection from "@/components/sections/HeroSection";
import FeatureGrid from "@/components/sections/FeatureGrid";

export default function HomePage() {
	return (
		<div className='space-y-12'>
			<HeroSection />
			<div className='max-w-7xl mx-auto w-full px-4'>
				<FeatureGrid />
			</div>
		</div>
	);
}
