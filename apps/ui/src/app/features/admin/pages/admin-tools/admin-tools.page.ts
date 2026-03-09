import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatDividerModule } from '@angular/material/divider';

type InventionCategory = 'scientific' | 'philosophical' | 'cultural';

type Invention = {
  id: string;
  name: string;
  category: InventionCategory;
  description: string;
  year: number;
  era: string;
  impact: number; // 0-100
};

@Component({
  selector: 'app-admin-tools',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatListModule,
  ],
  templateUrl: './admin-tools.page.html',
  styleUrl: './admin-tools.page.scss',
})
export class AdminToolsPage {
  scientific = signal<Invention[]>([
    {
      id: 'sci-1',
      name: 'Wheel',
      category: 'scientific',
      description: 'Revolutionary transportation tool',
      year: 142,
      era: 'ancient',
      impact: 95,
    },
    {
      id: 'sci-2',
      name: 'Agriculture',
      category: 'scientific',
      description: 'Systematic food production',
      year: 218,
      era: 'ancient',
      impact: 98,
    },
    {
      id: 'sci-3',
      name: 'Metallurgy',
      category: 'scientific',
      description: 'Working with metals',
      year: 376,
      era: 'ancient',
      impact: 87,
    },
    {
      id: 'sci-4',
      name: 'Writing System',
      category: 'scientific',
      description: 'Recorded communication',
      year: 445,
      era: 'ancient',
      impact: 92,
    },
  ]);

  philosophical = signal<Invention[]>([
    {
      id: 'phi-1',
      name: 'Ethics Framework',
      category: 'philosophical',
      description: 'Moral reasoning system',
      year: 312,
      era: 'ancient',
      impact: 76,
    },
    {
      id: 'phi-2',
      name: 'Logic System',
      category: 'philosophical',
      description: 'Formal reasoning',
      year: 398,
      era: 'ancient',
      impact: 81,
    },
    {
      id: 'phi-3',
      name: 'Social Contract',
      category: 'philosophical',
      description: 'Theory of governance',
      year: 567,
      era: 'medieval',
      impact: 73,
    },
  ]);

  cultural = signal<Invention[]>([
    {
      id: 'cul-1',
      name: 'Musical Notation',
      category: 'cultural',
      description: 'System for recording music',
      year: 289,
      era: 'ancient',
      impact: 68,
    },
    {
      id: 'cul-2',
      name: 'Epic Poetry',
      category: 'cultural',
      description: 'Narrative storytelling tradition',
      year: 334,
      era: 'ancient',
      impact: 72,
    },
    {
      id: 'cul-3',
      name: 'Theater',
      category: 'cultural',
      description: 'Dramatic performance art',
      year: 421,
      era: 'ancient',
      impact: 69,
    },
    {
      id: 'cul-4',
      name: 'Visual Perspective',
      category: 'cultural',
      description: 'Artistic depth technique',
      year: 512,
      era: 'medieval',
      impact: 64,
    },
  ]);

  columns = computed(() => [
    {
      title: 'Scientific',
      category: 'scientific' as const,
      items: this.scientific(),
      icon: 'science',
    },
    {
      title: 'Philosophical',
      category: 'philosophical' as const,
      items: this.philosophical(),
      icon: 'psychology',
    },
    {
      title: 'Cultural',
      category: 'cultural' as const,
      items: this.cultural(),
      icon: 'diversity_3',
    },
  ]);

  generateInvention(category: InventionCategory): void {
    const newInvention: Invention = {
      id: `${category}-${Date.now()}`,
      name: `New ${category} discovery`,
      category,
      description: 'Generated invention',
      year: 600,
      era: 'medieval',
      impact: Math.floor(Math.random() * 50) + 50,
    };

    if (category === 'scientific')
      this.scientific.update((prev) => [...prev, newInvention]);
    if (category === 'philosophical')
      this.philosophical.update((prev) => [...prev, newInvention]);
    if (category === 'cultural')
      this.cultural.update((prev) => [...prev, newInvention]);
  }
}
