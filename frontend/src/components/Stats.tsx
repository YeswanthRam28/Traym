/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useRef, useState } from 'react';
import ScrollReveal from './ScrollReveal';

interface CounterProps {
  end: number;
  duration?: number;
  decimals?: number;
  suffix?: string;
  isInfinitySpecial?: boolean;
}

function Counter({ end, duration = 1200, decimals = 0, suffix = '', isInfinitySpecial = false }: CounterProps) {
  const [count, setCount] = useState(0);
  const [hasTriggered, setHasTriggered] = useState(false);
  const [showInfin, setShowInfin] = useState(false);
  const elementRef = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setHasTriggered(true);
          observer.unobserve(entry.target);
        }
      },
      { threshold: 0.1 }
    );

    const currentRef = elementRef.current;
    if (currentRef) {
      observer.observe(currentRef);
    }

    return () => {
      if (currentRef) {
        observer.unobserve(currentRef);
      }
    };
  }, []);

  useEffect(() => {
    if (!hasTriggered) return;

    let startTimestamp: number | null = null;
    const step = (timestamp: number) => {
      if (!startTimestamp) startTimestamp = timestamp;
      const progress = Math.min((timestamp - startTimestamp) / duration, 1);
      
      // cubic ease-out
      const easeProgress = 1 - Math.pow(1 - progress, 3);
      const currentVal = easeProgress * end;
      
      setCount(currentVal);

      if (progress < 1) {
        window.requestAnimationFrame(step);
      } else {
        setCount(end);
        if (isInfinitySpecial) {
          setShowInfin(true);
        }
      }
    };

    window.requestAnimationFrame(step);
  }, [hasTriggered, end, duration, isInfinitySpecial]);

  if (showInfin && isInfinitySpecial) {
    return (
      <span className="text-[#D6FF00] scale-110 duration-500 ease-out inline-block filter drop-shadow-[0_0_10px_rgba(214,255,0,0.3)]">
        ∞
      </span>
    );
  }

  return (
    <span ref={elementRef}>
      {count.toFixed(decimals)}
      {suffix}
    </span>
  );
}

export default function Stats() {
  return (
    <section id="stats" className="bg-[#080808] py-16 border-b border-[#2A2A2A]/20">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <ScrollReveal className="grid grid-cols-1 md:grid-cols-3 gap-12 text-center items-center justify-center">
          {/* Stat 1: Members */}
          <div className="flex flex-col items-center">
            <h4 className="font-bebas text-7xl sm:text-8xl text-[#D6FF00] tracking-wider leading-none select-none">
              <Counter end={100} decimals={0} suffix="+" />
            </h4>
            <span className="font-space text-[11px] tracking-[0.25em] text-[#555555] uppercase mt-2">
              Members (Strict)
            </span>
          </div>

          {/* Stat 2: Rating */}
          <div className="flex flex-col items-center border-y md:border-y-0 md:border-x border-[#1A1A1A] py-8 md:py-0">
            <h4 className="font-bebas text-7xl sm:text-8xl text-[#D6FF00] tracking-wider leading-none select-none">
              <Counter end={4.8} decimals={1} suffix="★" />
            </h4>
            <span className="font-space text-[11px] tracking-[0.25em] text-[#555555] uppercase mt-2">
              Member Rating
            </span>
          </div>

          {/* Stat 3: Gains Special */}
          <div className="flex flex-col items-center">
            <h4 className="font-bebas text-7xl sm:text-8xl text-[#D6FF00] tracking-wider leading-none select-none transition-transform duration-300">
              <Counter end={999} decimals={0} isInfinitySpecial={true} />
            </h4>
            <span className="font-space text-[11px] tracking-[0.25em] text-[#555555] uppercase mt-2">
              Muscular Gains
            </span>
          </div>
        </ScrollReveal>
      </div>
    </section>
  );
}
