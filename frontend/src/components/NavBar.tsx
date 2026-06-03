/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';

interface NavBarProps {
  onDownloadClick: (e: React.MouseEvent<HTMLAnchorElement>) => void;
}

export default function NavBar({ onDownloadClick }: NavBarProps) {
  return (
    <nav
      id="traym-nav"
      className="sticky top-0 z-40 bg-[#080808]/80 backdrop-blur-md transition-colors duration-300 w-full"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 sm:h-20">
          {/* Left: TRAYM wordmark */}
          <div className="flex-shrink-0">
            <a href="#" className="group flex items-center space-x-1 outline-none">
              <span className="font-bebas text-3xl sm:text-4xl text-[#D6FF00] tracking-[0.06em] leading-none select-none duration-200 transition-transform group-hover:scale-105">
                TRAYM
              </span>
              <span className="font-space text-[9px] tracking-widest text-[#666666] self-end mb-1 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                [V1.0]
              </span>
            </a>
          </div>

          {/* Right: Sharp Download APK CTA */}
          <div className="flex items-center">
            <a
              href="#download"
              onClick={onDownloadClick}
              className="bg-[#D6FF00] text-[#080808] hover:bg-[#D6FF00]/95 hover:shadow-[0_0_15px_rgba(214,255,0,0.35)] transition-all duration-200 font-dm font-medium text-xs sm:text-sm tracking-wider uppercase px-4 sm:px-6 py-2.5 sm:py-3 select-none"
              style={{ borderRadius: '0px' }}
            >
              DOWNLOAD APK
            </a>
          </div>
        </div>
      </div>
    </nav>
  );
}
