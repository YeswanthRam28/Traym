/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useState } from 'react';

export default function Loader() {
  const [stage, setStage] = useState<'loading' | 'sliding' | 'done'>('loading');

  useEffect(() => {
    // Prevent scrolling while loading
    document.body.style.overflow = 'hidden';

    // After 1.2s, start sliding up
    const timer1 = setTimeout(() => {
      setStage('sliding');
      document.body.style.overflow = '';
    }, 1200);

    // After 2.0s (1.2s wait + 800ms slide transition), unmount loader from tree
    const timer2 = setTimeout(() => {
      setStage('done');
    }, 2000);

    return () => {
      clearTimeout(timer1);
      clearTimeout(timer2);
      document.body.style.overflow = '';
    };
  }, []);

  if (stage === 'done') return null;

  return (
    <div
      id="traym-loader"
      className={`fixed inset-0 z-100 flex flex-col items-center justify-center bg-[#080808] transition-transform duration-800 ease-[cubic-bezier(0.85,0,0.15,1)] ${
        stage === 'sliding' ? '-translate-y-full' : 'translate-y-0'
      }`}
    >
      <div className="relative flex flex-col items-center max-w-sm px-4">
        {/* Text Container with Sweep Accent */}
        <div className="relative py-3 overflow-hidden">
          {/* TRAYM wordmark and scanline */}
          <h1 className="font-bebas text-8xl sm:text-9xl tracking-[0.1em] text-[#D6FF00] leading-none mb-1 select-none">
            TRAYM
          </h1>
          {/* Scanline element */}
          <div className="absolute left-[-20%] right-[-20%] h-[3px] bg-[#D6FF00] shadow-[0_0_12px_#D6FF00,0_0_4px_#D6FF00] animate-scan pointer-events-none" />
        </div>

        {/* Console loading label */}
        <div className="flex items-center space-x-2 mt-4">
          <span className="w-1.5 h-1.5 bg-[#D6FF00] animate-ping" />
          <p className="font-space text-xs tracking-[0.25em] text-[#666666] uppercase">
            CONNECTING CORE CLIENT
          </p>
        </div>
      </div>
    </div>
  );
}
