/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef, useState } from 'react';

interface HeroProps {
  onDownloadClick: (e: React.MouseEvent<HTMLAnchorElement>) => void;
}

export default function Hero({ onDownloadClick }: HeroProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLAnchorElement>(null);

  const [scrollY, setScrollY] = useState(0);
  const [tilt, setTilt] = useState({ x: 0, y: 0 });
  const [btnOffset, setBtnOffset] = useState({ x: 0, y: 0 });
  const [hasEntered, setHasEntered] = useState(false);
  const [isTouchDevice, setIsTouchDevice] = useState(false);

  useEffect(() => {
    // Detect Touch Device
    const touchCheck = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
    setIsTouchDevice(touchCheck);

    // Synchronize mask reveal with loader finish
    const entryTimer = setTimeout(() => {
      setHasEntered(true);
    }, 1300);

    // Track scroll for 0.4x parallax
    const handleScroll = () => {
      setScrollY(window.scrollY);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });

    return () => {
      clearTimeout(entryTimer);
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (isTouchDevice) return;

    // 1. Tilt Calculation on Hero Illustration
    if (containerRef.current) {
      const rect = containerRef.current.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      const centerX = rect.width / 2;
      const centerY = rect.height / 2;

      // Max tilt angle of ±8 degrees
      const rotY = ((x - centerX) / centerX) * 8;
      const rotX = -((y - centerY) / centerY) * 8;
      setTilt({ x: rotX, y: rotY });
    }

    // 2. Magnetic Attraction on Hero Button (100px radius, 8px cap)
    if (buttonRef.current) {
      const btnRect = buttonRef.current.getBoundingClientRect();
      const btnCenterX = btnRect.left + btnRect.width / 2;
      const btnCenterY = btnRect.top + btnRect.height / 2;

      const deltaX = e.clientX - btnCenterX;
      const deltaY = e.clientY - btnCenterY;
      const dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

      if (dist < 100) {
        const pull = (100 - dist) / 100; // Stronger pull closer to center
        const rawX = deltaX * 0.15 * pull;
        const rawY = deltaY * 0.15 * pull;
        // Cap offset at 8px
        const capX = Math.max(-8, Math.min(8, rawX));
        const capY = Math.max(-8, Math.min(8, rawY));
        setBtnOffset({ x: capX, y: capY });
      } else {
        setBtnOffset({ x: 0, y: 0 });
      }
    }
  };

  const handleMouseLeave = () => {
    setTilt({ x: 0, y: 0 });
    setBtnOffset({ x: 0, y: 0 });
  };

  // Parallax displacement: 0.4x scroll speed
  const parallaxTranslation = scrollY * 0.4;

  return (
    <section
      id="hero"
      ref={containerRef}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      className="relative min-h-[calc(100vh-5rem)] flex items-center justify-center overflow-hidden bg-[#080808] px-4 sm:px-6 lg:px-8 py-12 select-none"
    >
      {/* BACKGROUND GRAPHIC (3D Barbell SVG + Perspective Tilt & Parallax) */}
      <div
        className="absolute inset-0 flex items-center justify-center opacity-35 sm:opacity-50 pointer-events-none select-none z-0"
        style={{
          transform: `translateY(${parallaxTranslation}px) perspective(1000px) rotateX(${tilt.x}deg) rotateY(${tilt.y}deg)`,
          transition: 'transform 100ms ease-out',
        }}
      >
        <svg
          width="100%"
          height="100%"
          viewBox="0 0 1200 800"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="w-[140%] h-[140%] sm:w-[110%] sm:h-[110%] max-w-7xl"
        >
          {/* Blueprint Grid lines */}
          <defs>
            <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#1F1F1F" strokeWidth="1" />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#grid)" />

          {/* Central isometric plate ring system */}
          <g transform="translate(600, 400)">
            {/* Outer Ring outline */}
            <circle cx="0" cy="0" r="280" stroke="#1A1A1A" strokeWidth="12" />
            <circle cx="0" cy="0" r="280" stroke="#D6FF00" strokeWidth="1" strokeDasharray="30 10 10 10" />

            {/* Inner Plate silhouette bounds */}
            <circle cx="0" cy="0" r="200" fill="#111111" stroke="#2A2A2A" strokeWidth="2" />
            <circle cx="0" cy="0" r="140" stroke="#2A2A2A" strokeWidth="1" strokeDasharray="5 5" />
            
            {/* Mechanical Weight Markings */}
            <path d="M -90 -45 L -100 -50" stroke="#D6FF00" strokeWidth="2" />
            <path d="M 90 45 L 100 50" stroke="#D6FF00" strokeWidth="2" />
            <text x="-120" y="-55" fill="#D6FF00" className="font-space text-[12px] tracking-wider" opacity="0.6">25 KG</text>
            <text x="75" y="75" fill="#666666" className="font-space text-[10px] tracking-widest">TRAYM AUTOMOTIVE CORE</text>

            {/* Simulated 3D Barbell Shaft */}
            <g transform="rotate(-25)">
              {/* Shaft Bar */}
              <rect x="-560" y="-12" width="1120" height="24" fill="#0D0D0D" stroke="#2A2A2A" strokeWidth="2" />
              <line x1="-560" y1="0" x2="560" y2="0" stroke="#D6FF00" strokeWidth="1" strokeDasharray="15 15" opacity="0.4" />
              
              {/* Collar Sleeves Left */}
              <rect x="-190" y="-20" width="30" height="40" fill="#151515" stroke="#D6FF00" strokeWidth="2" />
              <rect x="-160" y="-28" width="5" height="56" fill="#D6FF00" />
              <rect x="-420" y="-18" width="230" height="36" fill="#111111" stroke="#2A2A2A" strokeWidth="2" />
              
              {/* Knurling details left */}
              <path d="M -380 -18 L -360 18 M -370 -18 L -350 18 M -360 -18 L -340 18" stroke="#1A1A1A" strokeWidth="1" />
              <path d="M -300 -18 L -280 18 M -290 -18 L -270 18 M -280 -18 L -260 18" stroke="#1A1A1A" strokeWidth="1" />

              {/* Collar Sleeves Right */}
              <rect x="160" y="-20" width="30" height="40" fill="#151515" stroke="#D6FF00" strokeWidth="2" />
              <rect x="155" y="-28" width="5" height="56" fill="#D6FF00" />
              <rect x="190" y="-18" width="230" height="36" fill="#111111" stroke="#2A2A2A" strokeWidth="2" />
              
              {/* Knurling details right */}
              <path d="M 260 -18 L 280 18 M 270 -18 L 290 18 M 280 -18 L 300 18" stroke="#1A1A1A" strokeWidth="1" />
              <path d="M 320 -18 L 340 18 M 330 -18 L 350 18 M 340 -18 L 360 18" stroke="#1A1A1A" strokeWidth="1" />

              {/* Weight Plates Left stacked */}
              <rect x="-240" y="-110" width="16" height="220" fill="#111111" stroke="#2A2A2A" strokeWidth="2" />
              <rect x="-222" y="-110" width="16" height="220" fill="#111111" stroke="#D6FF00" strokeWidth="1.5" />
              <rect x="-204" y="-95" width="12" height="190" fill="#111111" stroke="#2A2A2A" strokeWidth="2" />

              {/* Weight Plates Right stacked */}
              <rect x="224" y="-110" width="16" height="220" fill="#111111" stroke="#2A2A2A" strokeWidth="2" />
              <rect x="206" y="-110" width="16" height="220" fill="#111111" stroke="#D6FF00" strokeWidth="1.5" />
              <rect x="192" y="-95" width="12" height="190" fill="#111111" stroke="#2A2A2A" strokeWidth="2" />
            </g>

            {/* Center Lock Pin Assembly */}
            <circle cx="0" cy="0" r="35" fill="#0D0D0D" stroke="#D6FF00" strokeWidth="2" />
            <circle cx="0" cy="0" r="14" fill="#D6FF00" />
          </g>
          
          {/* Hex bolts around bounds */}
          <polygon points="150,150 160,140 170,155 160,170 145,160" fill="#1A1A1A" stroke="#2A2A2A" strokeWidth="1" />
          <polygon points="1050,650 1060,640 1070,655 1060,670 1045,660" fill="#1A1A1A" stroke="#2A2A2A" strokeWidth="1" />
        </svg>
      </div>

      {/* CORE CONTENT (With masked reveal transitions) */}
      <div className="relative max-w-5xl mx-auto text-center z-10 flex flex-col items-center">
        {/* Metric Label indicator */}
        <div
          className="mb-8 overflow-hidden"
          style={{
            clipPath: hasEntered ? 'inset(0 0% 0 0)' : 'inset(0 100% 0 0)',
            transition: 'clip-path 800ms cubic-bezier(0.16, 1, 0.3, 1) 150ms',
          }}
        >
          <span className="font-space text-xs tracking-[0.35em] text-[#D6FF00] bg-[#D6FF00]/10 px-3.5 py-1.5 border border-[#D6FF00]/10 uppercase">
            REPS_CORE V3 DEV_SYS ACTIVE
          </span>
        </div>

        {/* Huge Hero Title (200px+ display font) */}
        <div className="relative overflow-visible mb-6">
          <h1
            className="font-bebas text-[22vw] sm:text-[180px] md:text-[210px] lg:text-[250px] leading-none text-[#D6FF00] tracking-[-0.01em]"
            style={{
              clipPath: hasEntered ? 'inset(0 0% 0 0)' : 'inset(0 100% 0 0)',
              transition: 'clip-path 1200ms cubic-bezier(0.77, 0, 0.175, 1) 300ms',
            }}
          >
            TRAYM
          </h1>
        </div>

        {/* Tagline & Subtitle */}
        <div
          className="max-w-2xl mx-auto space-y-4 mb-10 text-center"
          style={{
            clipPath: hasEntered ? 'inset(0 0% 0 0)' : 'inset(0 100% 0 0)',
            transition: 'clip-path 1000ms cubic-bezier(0.16, 1, 0.3, 1) 500ms',
          }}
        >
          <p className="font-dm font-medium text-lg sm:text-2xl text-[#F2F2F2] tracking-wide">
            Traym V3. Your AI Coach & Log. Built for obsessive lifters.
          </p>
          <p className="font-dm text-sm sm:text-base text-[#666666] leading-relaxed max-w-lg mx-auto">
            Track every rep. Know every why. Decipher muscular fatigue. A highly private community engine for the dedicated few.
          </p>
        </div>

        {/* CTA Button with Magnetic Hover Offset */}
        <div
          className="flex justify-center"
          style={{
            clipPath: hasEntered ? 'inset(0 0% 0 0)' : 'inset(0 100% 0 0)',
            transition: 'clip-path 900ms cubic-bezier(0.16, 1, 0.3, 1) 650ms',
          }}
        >
          <a
            id="hero-magnetic-cta"
            ref={buttonRef}
            href="#download"
            onClick={onDownloadClick}
            className="relative bg-[#D6FF00] text-[#080808] hover:bg-[#D6FF00]/95 hover:shadow-[0_0_25px_rgba(214,255,0,0.45)] font-dm font-bold text-sm tracking-[0.1em] uppercase px-8 py-4.5 select-none duration-150 inline-block text-center cursor-pointer"
            style={{
              borderRadius: '0px',
              transform: `translate(${btnOffset.x}px, ${btnOffset.y}px)`,
              transition: isTouchDevice ? 'none' : 'transform 150ms ease-out, background-color 200ms, box-shadow 200ms',
            }}
          >
            GET THE APP (APK)
          </a>
        </div>

        {/* Scroll helper bar */}
        <div
          className="mt-16 flex flex-col items-center space-y-1 opacity-40 hover:opacity-100 transition-opacity duration-300"
          style={{
            clipPath: hasEntered ? 'inset(0 0% 0 0)' : 'inset(0 100% 0 0)',
            transition: 'clip-path 900ms cubic-bezier(0.16, 1, 0.3, 1) 800ms',
          }}
        >
          <span className="font-space text-[10px] tracking-widest text-[#666666]">SCROLL DOWN</span>
          <div className="w-[1px] h-10 bg-[#2A2A2A] relative overflow-hidden">
            <div className="absolute top-0 left-0 right-0 h-1/2 bg-[#D6FF00] animate-[bounce_1.8s_infinite]" />
          </div>
        </div>
      </div>
    </section>
  );
}
