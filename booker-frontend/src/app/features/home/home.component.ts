import { Component, OnInit } from '@angular/core';
import { SearchService, BusinessSearchResult, BusinessCategory } from '../../core/search/search.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  standalone: false,
})
export class HomeComponent implements OnInit {
  constructor(private readonly searchService: SearchService) {}

  searchQuery = '';
  selectedCategoryId: number | null = null;
  businesses: BusinessSearchResult[] = [];
  categories: BusinessCategory[] = [];
  loading = false;
  searched = false;

  readonly categoryIcons: Record<string, string> = {
    barbershop:     '💈',
    beauty_salon:   '💇',
    nail_studio:    '💅',
    spa_wellness:   '🧖',
    massage:        '💆',
    fitness_gym:    '🏋️',
    medical_clinic: '🏥',
    dental_clinic:  '🦷',
    photography:    '📷',
    car_service:    '🔧',
    coworking:      '💼',
    sports_court:   '🏅',
  };

  ngOnInit(): void {
    this.searchService.getCategories().subscribe(cats => (this.categories = cats));
  }

  search(): void {
    this.loading = true;
    this.searched = true;
    this.searchService
      .searchBusinesses({
        query: this.searchQuery || undefined,
        categoryId: this.selectedCategoryId ?? undefined,
      })
      .subscribe({
        next: res => {
          this.businesses = res.content;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  selectCategory(id: number | undefined): void {
    if (id == null) return;
    this.selectedCategoryId = this.selectedCategoryId === id ? null : id;
    this.search();
  }

  getCategoryId(name: string): number | undefined {
    return this.categories.find(c => c.name === name)?.id;
  }
}
