/**
 * StarRating.jsx - 星级评分（0-5 星，按达成率点亮，内联 SVG 立体星）
 *
 * 颜色规则（按点亮数量区分，章节标题、小节标题与英语单元共用）：
 *   1 星 → 绿；2 星 → 蓝；3 星 → 红；4 星 → 金；5 星 → 蓝白银渐变（流苏流光）
 * 未点亮的星为磨砂白。
 *
 * 达成率阈值与旧版一致：≥80 五星 / ≥60 四星 / ≥40 三星 / ≥20 二星 / >0 一星。
 * 全部为内联 SVG（无位图、无 filter），12px 下仍能看出星形与颜色。
 */

import { useRef } from 'react';

// 圆角五角星路径（外接圆半径 1，中心原点，尖角朝上）
const STAR_D =
  'M -0.0451,-0.8997 Q 0.0000,-1.0000 0.0451,-0.8997 L 0.2324,-0.4823 ' +
  'Q 0.2775,-0.3819 0.3869,-0.3701 L 0.8417,-0.3209 Q 0.9511,-0.3090 0.8695,-0.2352 ' +
  'L 0.5305,0.0720 Q 0.4490,0.1459 0.4715,0.2536 L 0.5653,0.7013 Q 0.5878,0.8090 0.4924,0.7543 ' +
  'L 0.0954,0.5268 Q 0.0000,0.4721 -0.0954,0.5268 L -0.4924,0.7543 Q -0.5878,0.8090 -0.5653,0.7013 ' +
  'L -0.4715,0.2536 Q -0.4490,0.1459 -0.5305,0.0720 L -0.8695,-0.2352 Q -0.9511,-0.3090 -0.8417,-0.3209 ' +
  'L -0.3869,-0.3701 Q -0.2775,-0.3819 -0.2324,-0.4823 Z';

// 1~4 星单色调（light → base → dark 渐变 + 深色底座做出立体感）
const LEVEL_TONES = {
  1: { light: '#eafaf1', base: '#82d4a6', dark: '#5cad80' },
  2: { light: '#e9f3fd', base: '#86b6ea', dark: '#618dc4' },
  3: { light: '#fdeaea', base: '#f3a7a5', dark: '#c97d7b' },
  4: { light: '#fff6dd', base: '#f7d98d', dark: '#d2ab50' },
};

// 5 星：蓝 → 白 → 银
const FULL_STOPS = [
  ['0%', '#0b5b91'],
  ['18%', '#1e9bd2'],
  ['38%', '#4fbde6'],
  ['52%', '#b8e2f5'],
  ['64%', '#ffffff'],
  ['80%', '#dceaf5'],
  ['100%', '#aec3d6'],
];

const FROST = { fill: '#dfe9f2', stroke: '#c6d4e2', shadow: '#cfdbe6' };

// 渐变 id 需全局唯一（同页可能同时存在多个 StarRating）
let uidSeq = 0;

/** 达成率（0-100）→ 点亮星数（0-5） */
function rateToStarCount(rate) {
  const r = Number(rate) || 0;
  if (r >= 80) return 5;
  if (r >= 60) return 4;
  if (r >= 40) return 3;
  if (r >= 20) return 2;
  return r > 0 ? 1 : 0;
}

export default function StarRating({ value = 0, size = 14, className = '' }) {
  const uidRef = useRef('');
  if (!uidRef.current) uidRef.current = `sr${++uidSeq}`;
  const uid = uidRef.current;
  const count = rateToStarCount(value);
  const gap = size >= 16 ? 2 : 1;

  return (
    <span
      className={`star-rating ${className}`.trim()}
      style={{ display: 'inline-flex', alignItems: 'center', gap, lineHeight: 1, whiteSpace: 'nowrap', verticalAlign: 'middle' }}
      role="img"
      aria-label={`${count} 星`}
    >
      {[0, 1, 2, 3, 4].map((i) => {
        const lit = i < count;
        const gid = `${uid}-${i}`;
        const tone = lit ? (count === 5 ? null : LEVEL_TONES[count]) : null;
        const stops = tone
          ? [['0%', tone.light], ['42%', tone.base], ['100%', tone.dark]]
          : FULL_STOPS;

        return (
          <svg
            key={i}
            width={size}
            height={size}
            viewBox="-1.06 -1.06 2.12 2.12"
            style={{ display: 'block', flexShrink: 0 }}
            aria-hidden="true"
          >
            <defs>
              <linearGradient id={`${gid}-g`} x1="0.18" y1="0" x2="0.82" y2="1">
                {stops.map(([offset, color]) => (
                  <stop key={offset} offset={offset} stopColor={color} />
                ))}
              </linearGradient>
              <radialGradient id={`${gid}-hi`} cx="0.5" cy="0.5" r="0.5">
                <stop offset="0%" stopColor="#ffffff" stopOpacity="0.8" />
                <stop offset="60%" stopColor="#ffffff" stopOpacity="0.35" />
                <stop offset="100%" stopColor="#ffffff" stopOpacity="0" />
              </radialGradient>
              <clipPath id={`${gid}-c`}>
                <path d={STAR_D} />
              </clipPath>
            </defs>

            {lit ? (
              <>
                {/* 立体底座 */}
                <path d={STAR_D} transform="translate(0 0.14)" fill={tone ? tone.dark : '#8ba6bb'} opacity="0.2" />
                <path
                  d={STAR_D}
                  transform="translate(0 0.085)"
                  fill={tone ? tone.dark : '#93aec2'}
                  stroke={tone ? tone.dark : '#93aec2'}
                  strokeWidth="0.06"
                  strokeLinejoin="round"
                />
                {/* 星体 */}
                <path
                  d={STAR_D}
                  fill={`url(#${gid}-g)`}
                  stroke={tone ? tone.light : '#aec9dd'}
                  strokeWidth="0.05"
                  strokeLinejoin="round"
                />
                {/* 高光与流光 */}
                <g clipPath={`url(#${gid}-c)`}>
                  <ellipse
                    cx="-0.16"
                    cy="-0.56"
                    rx="0.62"
                    ry="0.34"
                    fill={`url(#${gid}-hi)`}
                    transform="rotate(-16 -0.16 -0.56)"
                  />
                  {tone ? (
                    <circle cx="-0.44" cy="-0.62" r="0.09" fill="#ffffff" opacity="0.85" />
                  ) : (
                    <>
                      <path
                        d="M -0.95 0.02 Q -0.30 -0.22 0.50 -0.12 L 0.62 0.04 Q -0.10 -0.06 -0.95 0.22 Z"
                        fill="#ffffff"
                        opacity="0.16"
                      />
                      <path
                        d="M -0.58 0.40 Q 0 0.28 0.58 0.40"
                        stroke="#ffffff"
                        strokeWidth="0.03"
                        fill="none"
                        opacity="0.32"
                      />
                      <circle cx="-0.42" cy="-0.60" r="0.09" fill="#ffffff" opacity="0.9" />
                    </>
                  )}
                </g>
              </>
            ) : (
              <>
                <path d={STAR_D} transform="translate(0 0.08)" fill={FROST.shadow} opacity="0.5" />
                <path d={STAR_D} fill={FROST.fill} stroke={FROST.stroke} strokeWidth="0.05" strokeLinejoin="round" />
                <g clipPath={`url(#${gid}-c)`}>
                  <ellipse cx="-0.18" cy="-0.58" rx="0.55" ry="0.30" fill="#ffffff" opacity="0.7" />
                </g>
              </>
            )}
          </svg>
        );
      })}
    </span>
  );
}
