/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        teal: {
          900: '#0A3641',
          800: '#0F4C5C',
          700: '#1F6E8C',
          600: '#2A8BA8',
          100: '#E8EEF0',
          50: '#F4F7F8'
        },
        amber: {
          500: '#E07A5F',
          600: '#D06043'
        },
        gold: {
          500: '#F2A65A'
        }
      }
    },
  },
  plugins: [],
}
