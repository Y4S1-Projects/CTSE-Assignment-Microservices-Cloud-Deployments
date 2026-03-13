/** @type {import('tailwindcss').Config} */
module.exports = {
	content: ["./src/pages/**/*.{js,jsx,mdx}", "./src/components/**/*.{js,jsx,mdx}", "./src/app/**/*.{js,jsx,mdx}"],
	theme: {
		extend: {
			colors: {
				brand: {
					50: "#ecfdf3",
					100: "#d1fae5",
					200: "#a7f3d0",
					300: "#6ee7b7",
					400: "#34d399",
					500: "#10b981",
					600: "#059669",
					700: "#047857",
					800: "#065f46",
					900: "#064e3b",
				},
			},
			boxShadow: {
				soft: "0 10px 30px rgba(16, 185, 129, 0.15)",
			},
		},
	},
	plugins: [],
};
