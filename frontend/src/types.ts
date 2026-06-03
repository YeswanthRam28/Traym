/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { LucideIcon } from 'lucide-react';

export interface Feature {
  id: string;
  title: string;
  iconName: 'barbell' | 'brain' | 'trophy' | 'group';
  description: string;
}

export interface Stat {
  id: string;
  value: string;
  numericVal: number;
  suffix: string;
  label: string;
}
