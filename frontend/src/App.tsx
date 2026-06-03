/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import Loader from './components/Loader';
import NavBar from './components/NavBar';
import Hero from './components/Hero';
import Features from './components/Features';
import Stats from './components/Stats';
import Download from './components/Download';

export default function App() {
  const [loadingComplete, setLoadingComplete] = useState(false);

  // Smooth scroll handler to the Download section
  const handleDownloadClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    const target = document.getElementById('download');
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  return (
    <div className="min-h-screen bg-[#080808] text-[#F2F2F2] selection:bg-[#D6FF00] selection:text-[#080808]">
      {/* 1. CREATIVE INTRO SCANNING LOADER */}
      <Loader />

      {/* 2. STICKY BLUR NAV BAR */}
      <NavBar onDownloadClick={handleDownloadClick} />

      {/* MAIN SINGLE LANDING CANVAS */}
      <main className="relative z-10">
        {/* 3. RESPONSIVE HERO CONTAINER (Includes Parallax, 3D SVG Tilt & Magnetic CTA) */}
        <Hero onDownloadClick={handleDownloadClick} />

        {/* 4. FEATURE STRIP (Horizontal drag scroll with dim-toggle hover) */}
        <Features />

        {/* 5. COUNTERS & STATS PROOF (IntersectionObserver counts) */}
        <Stats />

        {/* 6. DOWNLOAD & DEVICE GRAPHIC */}
        <Download />
      </main>

      {/* MINIMALIST COMPACT FOOTER */}
      <footer className="bg-[#080808] border-t border-[#1a1a1a] py-8 text-center text-xs select-none">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4 font-space text-[10px] tracking-wider text-[#444]">
          <p>© {new Date().getFullYear()} TRAYM SERVICES. ALL MECHANICAL RIGHTS RESERVED.</p>
          <p className="text-[#333]">STRICTLY PRIVATE COMMUNITY CODEBASE / AUTH_ID: CR7_SYS_INT</p>
        </div>
      </footer>
    </div>
  );
}
