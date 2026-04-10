/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#7C3AED',
          soft: '#EDE9FE',
          dark: '#5B21B6',
          light: '#A78BFA',
        },
        surface: '#FFFFFF',
        page: '#F1F5F9',
        sidebar: {
          DEFAULT: '#0F172A',
          hover: '#1E293B',
          active: '#1E293B',
          border: '#1E293B',
          text: '#94A3B8',
          heading: '#64748B',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['"Plus Jakarta Sans"', 'Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        card: '16px',
        btn: '10px',
      },
      boxShadow: {
        soft: '0 1px 3px 0 rgba(0,0,0,0.04), 0 4px 16px -2px rgba(0,0,0,0.06)',
        card: '0 1px 2px 0 rgba(15,23,42,0.04), 0 12px 40px -12px rgba(15,23,42,0.12)',
        float: '0 20px 50px -20px rgba(76,29,149,0.15), 0 8px 16px -8px rgba(15,23,42,0.08)',
        glow: '0 0 0 3px rgba(124,58,237,0.15)',
        'inner-sm': 'inset 0 1px 2px rgba(0,0,0,0.06)',
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'auth-gradient': 'linear-gradient(135deg, #4F46E5 0%, #7C3AED 50%, #9333EA 100%)',
      },
      animation: {
        'fade-up': 'fadeUp 0.4s ease both',
        'slide-in': 'slideIn 0.3s ease both',
      },
      keyframes: {
        fadeUp: {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideIn: {
          '0%': { opacity: '0', transform: 'translateX(-8px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
      },
    },
  },
  plugins: [],
}
