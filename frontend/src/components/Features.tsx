/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useRef, useState, useEffect } from 'react';
import { Dumbbell, Activity, Trophy, Users, ArrowRight, Database, Layers } from 'lucide-react';
import MaskHeading from './MaskHeading';
import ScrollReveal from './ScrollReveal';

interface CardData {
  id: string;
  title: string;
  icon: any;
  desc: string;
  tag: string;
}

const CARDS: CardData[] = [
  {
    id: 'log',
    title: 'Log Every Rep',
    icon: Dumbbell,
    desc: 'Log with absolute mechanical precision. No filler, no bloated drop-downs. Designed for speed-loaded inputs between intense heavy working sets.',
    tag: 'LOG_ENGINE_v4',
  },
  {
    id: 'ai',
    title: 'AI Coach Reads You',
    icon: Activity,
    desc: 'Not simple stats. Your neural engine reads training volume, fatigue indices, and suggests micro-load adjustments on the fly based on true progression logs.',
    tag: 'COACH_NET_CORE',
  },
  {
    id: 'pr',
    title: 'PR Tracker',
    icon: Trophy,
    desc: 'Shatter your mechanical ceilings. Comprehensive record logs of absolute 1RMs, relative volume limits, and linear progression coefficients plotted instantly.',
    tag: 'PR_INDEX_ACTIVE',
  },
  {
    id: 'feed',
    title: 'Community Feed',
    icon: Users,
    desc: 'A private locker room of dedicated lifters. Share footage, feedback, and high-fives on true heavy pulls. Zero noise, zero fitness influencers.',
    tag: 'INNER_CIRCLE_NET',
  },
  {
    id: 'notion',
    title: 'Notion Sync',
    icon: Database,
    desc: 'Automatically push and sync all your verified workout logs into your personal Notion databases in real time. Absolute data sovereignty.',
    tag: 'BACKUP_PROTOCOL',
  },
  {
    id: 'sets',
    title: 'Advanced Sets',
    icon: Layers,
    desc: 'Instantly toggle sets into Drop Sets, Super Sets, or Warm-ups. The AI engine reads these context tags to accurately measure progressive overload.',
    tag: 'TACTICAL_SETS',
  },
];

export default function Features() {
  const containerRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [startX, setStartX] = useState(0);
  const [scrollLeftState, setScrollLeftState] = useState(0);
  const [hasScrolled, setHasScrolled] = useState(false);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  useEffect(() => {
    // Also fade out hint label if user scrolls manually
    const element = containerRef.current;
    if (!element) return;

    const checkScroll = () => {
      if (element.scrollLeft > 20) {
        setHasScrolled(true);
      }
    };

    element.addEventListener('scroll', checkScroll, { passive: true });
    return () => element.removeEventListener('scroll', checkScroll);
  }, []);

  // Desktop Mouse Drag Scroll Handlers
  const handleMouseDown = (e: React.MouseEvent) => {
    if (!containerRef.current) return;
    setIsDragging(true);
    setStartX(e.pageX - containerRef.current.offsetLeft);
    setScrollLeftState(containerRef.current.scrollLeft);
  };

  const handleMouseLeave = () => {
    setIsDragging(false);
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging || !containerRef.current) return;
    e.preventDefault();
    const x = e.pageX - containerRef.current.offsetLeft;
    const walk = (x - startX) * 1.5; // Drag sensitivity multiplier
    containerRef.current.scrollLeft = scrollLeftState - walk;
    if (Math.abs(walk) > 10) {
      setHasScrolled(true);
    }
  };

  return (
    <section id="features" className="bg-[#080808] py-20 px-4 sm:px-6 lg:px-8 border-y border-[#2A2A2A]/20">
      <div className="max-w-7xl mx-auto">
        {/* Header section info */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-12">
          <div>
            <span className="font-space text-xs tracking-[0.4em] text-[#666666] uppercase block mb-3">
              WHAT IT DOES
            </span>
            <MaskHeading as="h2" className="font-syne text-3xl sm:text-4xl lg:text-5xl font-extrabold text-[#F2F2F2] tracking-tight uppercase leading-none">
              BUILT TO LIFT.
            </MaskHeading>
          </div>

          {/* Interactive Drag Hint Label */}
          <div
            className={`mt-4 sm:mt-0 flex items-center space-x-3 text-[#D6FF00] font-space text-xs tracking-[0.15em] uppercase transition-opacity duration-500 ${
              hasScrolled ? 'opacity-0 pointer-events-none' : 'opacity-80 animate-pulse'
            }`}
          >
            <span>drag to explore</span>
            <ArrowRight className="w-4 h-4" />
          </div>
        </div>

        {/* Horizontal Drag Area wrapper */}
        <div
          ref={containerRef}
          onMouseDown={handleMouseDown}
          onMouseUp={handleMouseUp}
          onMouseLeave={handleMouseLeave}
          onMouseMove={handleMouseMove}
          className={`flex overflow-x-auto gap-6 pb-8 pt-4 no-scrollbar scroll-smooth cursor-grab ${
            isDragging ? 'cursor-grabbing select-none' : ''
          }`}
          style={{
            WebkitOverflowScrolling: 'touch',
          }}
        >
          {CARDS.map((card, idx) => {
            const IconComponent = card.icon;
            const isAnyHovered = hoveredIndex !== null;
            const isThisHovered = hoveredIndex === idx;

            return (
              <div key={card.id} className="flex-shrink-0">
                <ScrollReveal
                  delay={idx * 80}
                >
                <div
                  onMouseEnter={() => setHoveredIndex(idx)}
                  onMouseLeave={() => setHoveredIndex(null)}
                  className={`relative w-[280px] h-[340px] bg-[#111111] border border-[#2A2A2A] flex flex-col justify-between p-6 transition-all duration-300 ease-out select-none ${
                    isThisHovered 
                      ? 'scale-[1.03] border-t-[#D6FF00] shadow-[0_10px_30px_rgba(214,255,0,0.04)] z-10' 
                      : 'scale-100'
                  } ${
                    isAnyHovered && !isThisHovered ? 'opacity-65 blur-[0.4px]' : 'opacity-100'
                  }`}
                  style={{ borderRadius: '0px' }}
                >
                  {/* Top: Metadata & Icon */}
                  <div>
                    <div className="flex items-center justify-between mb-6">
                      <span className="font-space text-[10px] tracking-widest text-[#666666] select-none">
                        {card.tag}
                      </span>
                      <IconComponent
                        className={`w-6 h-6 transition-transform duration-300 ${
                          isThisHovered ? 'text-[#D6FF00] scale-110 rotate-3' : 'text-[#666666]'
                        }`}
                      />
                    </div>

                    <h3 className="font-syne text-xl text-[#F2F2F2] uppercase font-bold tracking-tight mb-3">
                      {card.title}
                    </h3>

                    <p className="font-dm text-xs text-[#666666] leading-relaxed">
                      {card.desc}
                    </p>
                  </div>

                  {/* Bottom Highlight Animation edge */}
                  <div className="relative w-full h-1 bg-[#1a1a1a]">
                    <div
                      className="absolute top-0 left-0 h-full bg-[#D6FF00] transition-all duration-300 ease-out"
                      style={{
                        width: isThisHovered ? '100%' : '0%',
                      }}
                    />
                  </div>
                </div>
              </ScrollReveal>
            </div>);
          })}
        </div>
      </div>
    </section>
  );
}
