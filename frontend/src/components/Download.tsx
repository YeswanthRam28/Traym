/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';
import { Wifi, Battery, Cpu, Lock, Download as DownloadIcon, Database, Key, Link2, Smartphone, CheckCircle2, ChevronRight, Copy, ClipboardCheck } from 'lucide-react';
import MaskHeading from './MaskHeading';
import ScrollReveal from './ScrollReveal';

const NOTION_STEPS = [
  {
    num: '01',
    icon: Database,
    title: 'Create a Notion Page',
    desc: 'Open Notion and create a brand new, empty page. Name it "Traym Fitness Log" (or anything you like).',
    detail: null,
  },
  {
    num: '02',
    icon: Smartphone,
    title: 'Connect in Traym',
    desc: 'Open the Traym app on your phone. Go to Profile → Connect with Notion.',
    detail: null,
  },
  {
    num: '03',
    icon: Key,
    title: 'Grant Access',
    desc: 'When the Notion authorization page opens, sign in and select ONLY the new "Traym Fitness Log" page you just created. Hit "Allow Access".',
    detail: null,
  },
  {
    num: '04',
    icon: CheckCircle2,
    title: 'Auto-Magic Setup',
    desc: 'The app will automatically build the entire database with the exact columns and schema it needs inside that page. You don\'t have to build any columns yourself!',
    detail: (
      <div className="mt-3 flex items-center gap-2 font-space text-[10px] text-[#D6FF00]">
        <CheckCircle2 className="w-3.5 h-3.5 flex-shrink-0" />
        <span>Your workouts will now sync automatically after every session!</span>
      </div>
    ),
  },
];

export default function Download() {
  const [activeTab, setActiveTab] = useState<'apk' | 'notion'>('apk');

  return (
    <section id="download" className="bg-[#111111] py-24 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Background design accents */}
      <div className="absolute right-0 bottom-0 w-96 h-96 bg-[#D6FF00]/[0.015] rounded-full blur-3xl pointer-events-none" />
      <div className="absolute left-10 top-10 w-64 h-64 bg-white/[0.01] rounded-full blur-2xl pointer-events-none" />

      <div className="max-w-7xl mx-auto">
        {/* Section Label */}
        <div className="mb-10">
          <span className="font-space text-xs tracking-[0.4em] text-[#666666] uppercase block mb-4">
            AUTHENTIQUE DISTRIBUTION
          </span>
          <MaskHeading as="h2" className="font-bebas text-6xl sm:text-7xl lg:text-8xl text-[#F2F2F2] tracking-tight uppercase leading-none">
            GET TRAYM
          </MaskHeading>
        </div>

        {/* Tab Switcher */}
        <div className="flex border-b border-[#2A2A2A] mb-12 max-w-md">
          <button
            onClick={() => setActiveTab('apk')}
            className={`flex items-center gap-2 px-6 py-3 font-space text-xs tracking-[0.2em] uppercase transition-all duration-200 border-b-2 -mb-px ${
              activeTab === 'apk'
                ? 'border-[#D6FF00] text-[#D6FF00]'
                : 'border-transparent text-[#555] hover:text-[#F2F2F2]'
            }`}
          >
            <DownloadIcon className="w-3.5 h-3.5" />
            Get APK
          </button>
          <button
            onClick={() => setActiveTab('notion')}
            className={`flex items-center gap-2 px-6 py-3 font-space text-xs tracking-[0.2em] uppercase transition-all duration-200 border-b-2 -mb-px ${
              activeTab === 'notion'
                ? 'border-[#D6FF00] text-[#D6FF00]'
                : 'border-transparent text-[#555] hover:text-[#F2F2F2]'
            }`}
          >
            <Database className="w-3.5 h-3.5" />
            Setup Notion
          </button>
        </div>

        {/* TAB: GET APK */}
        {activeTab === 'apk' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 lg:gap-8 items-center">

            {/* LEFT: TEXT & CTA */}
            <div className="lg:col-span-6 flex flex-col justify-center space-y-8 text-left order-2 lg:order-1">
              <p className="font-dm text-sm sm:text-base text-[#666666] leading-relaxed max-w-md">
                Android only. Community builds only. We don&apos;t upload to consumer stores. We compile direct, tamper-proof binaries for active lifters of our community.
              </p>

              {/* Download CTA */}
              <div className="w-full sm:max-w-md">
                <a
                  href="/Traym.apk"
                  download="Traym.apk"
                  className="group relative flex items-center justify-center space-x-3 w-full h-[60px] bg-[#D6FF00] text-[#080808] hover:bg-transparent hover:text-[#D6FF00] border-2 border-[#D6FF00] font-syne font-bold text-sm tracking-[0.15em] uppercase transition-all duration-200 select-none cursor-pointer"
                  style={{ borderRadius: '0px' }}
                >
                  <DownloadIcon className="w-5 h-5 transition-transform duration-300 group-hover:-translate-y-0.5" />
                  <span>DOWNLOAD DIRECT APK (V1.0.4)</span>
                </a>
              </div>

              {/* Technical Meta */}
              <div className="border-t border-[#2A2A2A] pt-6 grid grid-cols-2 gap-4 max-w-md">
                <div className="flex items-start space-x-2">
                  <Cpu className="w-4 h-4 text-[#D6FF00] mt-0.5 flex-shrink-0" />
                  <div>
                    <p className="font-space text-[10px] text-[#F2F2F2] tracking-wide uppercase font-bold">Architecture</p>
                    <p className="font-space text-[10px] text-[#555555]">arm64-v8a / v7a</p>
                  </div>
                </div>
                <div className="flex items-start space-x-2">
                  <Lock className="w-4 h-4 text-[#D6FF00] mt-0.5 flex-shrink-0" />
                  <div>
                    <p className="font-space text-[10px] text-[#F2F2F2] tracking-wide uppercase font-bold">Min Android</p>
                    <p className="font-space text-[10px] text-[#555555]">Android 8.0+ (API 26)</p>
                  </div>
                </div>
              </div>

              {/* Sideload instructions */}
              <div className="max-w-md bg-[#0d0d0d] border border-[#2A2A2A] p-4 space-y-2">
                <p className="font-space text-[10px] text-[#D6FF00] tracking-widest uppercase">How to Install</p>
                <div className="space-y-1.5 font-space text-[10px] text-[#555]">
                  <div className="flex items-start gap-2"><ChevronRight className="w-3 h-3 mt-0.5 flex-shrink-0 text-[#D6FF00]" /><span>Download the APK to your Android device</span></div>
                  <div className="flex items-start gap-2"><ChevronRight className="w-3 h-3 mt-0.5 flex-shrink-0 text-[#D6FF00]" /><span>Go to Settings → Security → Enable <span className="text-[#F2F2F2]">"Install unknown apps"</span></span></div>
                  <div className="flex items-start gap-2"><ChevronRight className="w-3 h-3 mt-0.5 flex-shrink-0 text-[#D6FF00]" /><span>Open the downloaded APK file and tap Install</span></div>
                  <div className="flex items-start gap-2"><ChevronRight className="w-3 h-3 mt-0.5 flex-shrink-0 text-[#D6FF00]" /><span>Sign in with Google and start logging</span></div>
                </div>
              </div>
            </div>

            {/* RIGHT: PHONE MOCKUP */}
            <div className="lg:col-span-6 flex justify-center items-center order-1 lg:order-2">
              <ScrollReveal className="relative group">
                <div className="absolute inset-0 bg-[#D6FF00]/10 rounded-[38px] blur-3xl opacity-40 group-hover:opacity-75 transition-opacity duration-500 pointer-events-none" />
                <div className="relative w-[290px] h-[580px] bg-[#0c0c0c] border-[10px] border-[#222] rounded-[42px] shadow-[0_25px_60px_-15px_rgba(0,0,0,0.9),0_0_30px_rgba(214,255,0,0.06)] overflow-hidden flex flex-col justify-between p-5 select-none hover:shadow-[0_25px_65px_-10px_rgba(214,255,0,0.12)] transition-shadow duration-500">
                  <div className="absolute top-0 left-1/2 -translate-x-1/2 w-32 h-6 bg-[#222] rounded-b-2xl z-30 flex items-start justify-center">
                    <div className="w-2.5 h-2.5 bg-[#0a0a0a] border border-[#333] rounded-full mt-1.5" />
                  </div>
                  <div className="flex justify-between items-center text-[#444] font-space text-[10px] mt-2 px-3 z-10">
                    <span className="font-bold tracking-tight text-[#666]">14:22</span>
                    <div className="flex items-center space-x-1.5 text-[#666]">
                      <div className="flex space-x-0.5 items-center">
                        <div className="w-[3px] h-[5px] bg-[#666]" />
                        <div className="w-[3px] h-[7px] bg-[#666]" />
                        <div className="w-[3px] h-[9px] bg-[#D6FF00]" />
                      </div>
                      <Wifi className="w-3 h-3" />
                      <Battery className="w-3.5 h-3.5" />
                    </div>
                  </div>
                  <div className="flex-1 flex flex-col items-center justify-center relative mt-4">
                    <div className="absolute inset-0 bg-radial from-[#D6FF00]/[0.08] to-transparent pointer-events-none" />
                    <div className="relative text-center z-10 flex flex-col items-center">
                      <h3 className="font-bebas text-6xl text-[#D6FF00] tracking-[0.12em] leading-none mb-2">TRAYM</h3>
                      <p className="font-dm font-medium text-[11px] text-[#666666] tracking-wide uppercase">Your AI Coach & Log</p>
                    </div>
                  </div>
                  <div className="flex justify-center items-center pb-2 pt-4">
                    <div className="w-24 h-1 bg-[#222] rounded-full" />
                  </div>
                </div>
              </ScrollReveal>
            </div>
          </div>
        )}

        {/* TAB: SETUP NOTION */}
        {activeTab === 'notion' && (
          <div className="max-w-3xl">
            <p className="font-dm text-sm text-[#666666] leading-relaxed mb-10 max-w-xl">
              Traym can automatically sync every workout to a Notion database, giving you a beautiful, queryable log of your entire training history. It takes 15 seconds to set up.
            </p>

            <div className="space-y-0">
              {NOTION_STEPS.map((step, idx) => {
                const IconComponent = step.icon;
                return (
                  <div key={step.num} className="relative flex gap-6 pb-10 last:pb-0">
                    {/* Vertical connector line */}
                    {idx < NOTION_STEPS.length - 1 && (
                      <div className="absolute left-[19px] top-10 w-[1px] h-full bg-[#2A2A2A]" />
                    )}

                    {/* Step number bubble */}
                    <div className="flex-shrink-0 w-10 h-10 bg-[#0d0d0d] border border-[#2A2A2A] flex items-center justify-center z-10">
                      <span className="font-space text-[10px] text-[#D6FF00] font-bold">{step.num}</span>
                    </div>

                    {/* Content */}
                    <div className="flex-1 pt-1.5">
                      <div className="flex items-center gap-2 mb-2">
                        <IconComponent className="w-4 h-4 text-[#D6FF00]" />
                        <h3 className="font-syne font-bold text-sm text-[#F2F2F2] uppercase tracking-wide">{step.title}</h3>
                      </div>
                      <p className="font-dm text-xs text-[#666] leading-relaxed">{step.desc}</p>
                      {step.detail}

                    </div>
                  </div>
                );
              })}
            </div>
            {/* CTA to switch to APK tab */}
            <div className="mt-12 border-t border-[#2A2A2A] pt-8">
              <p className="font-space text-[11px] text-[#555] mb-4 tracking-wider">READY TO GO? GET THE APP FIRST.</p>
              <button
                onClick={() => setActiveTab('apk')}
                className="flex items-center gap-3 bg-[#D6FF00] text-[#080808] font-syne font-bold text-xs tracking-[0.15em] uppercase px-6 py-3 hover:bg-[#D6FF00]/90 transition-colors duration-200"
                style={{ borderRadius: '0px' }}
              >
                <DownloadIcon className="w-4 h-4" />
                Download APK
              </button>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
