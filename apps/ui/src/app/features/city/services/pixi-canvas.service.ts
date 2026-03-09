import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HumanOutput } from '@api';

// PixiJS types - will be dynamically imported
type PIXI = typeof import('pixi.js');
type PIXIApplication = import('pixi.js').Application;
type PIXIText = import('pixi.js').Text;

export interface HumanSprite {
  human: HumanOutput;
  sprite: PIXIText;
  hasCollision?: boolean;
  targetX?: number;
  targetY?: number;
}

@Injectable({
  providedIn: 'root'
})
export class PixiCanvasService {
  private platformId = inject(PLATFORM_ID);
  private pixiLib!: PIXI;
  private app!: PIXIApplication;
  private humanSprites: HumanSprite[] = [];
  private pendingHumans: HumanOutput[] = [];
  private resizeObserver?: ResizeObserver;

  async initialize(container: HTMLElement): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    try {
      this.pixiLib = await import('pixi.js');
      const PIXI = this.pixiLib;

      this.app = new PIXI.Application();
      await this.app.init({
        width: container.clientWidth,
        height: container.clientHeight,
        backgroundColor: 0x0f172a,
        backgroundAlpha: 0.85,
        resolution: window.devicePixelRatio || 1,
        antialias: true,
      });

      container.appendChild(this.app.canvas as HTMLCanvasElement);
      this.setupResizeObserver(container);
      this.setupTicker();
      this.processPendingHumans();
    } catch (error) {
      console.error('Error initializing PixiJS:', error);
    }
  }

  private setupResizeObserver(container: HTMLElement): void {
    // Use a single observer and rAF to avoid ResizeObserver loop errors
    this.resizeObserver?.disconnect();
    this.resizeObserver = new ResizeObserver(() => {
      if (!this.app) return;
      const width = container.clientWidth;
      const height = container.clientHeight;
      if (!width || !height) return;
      requestAnimationFrame(() => {
        this.app?.renderer?.resize(width, height);
    });
    });
    this.resizeObserver.observe(container);
  }

  private setupTicker(): void {
    this.app.ticker.add(() => {
      const speed = 0.1;
      this.humanSprites.forEach((h) => {
        if (!h.hasCollision) {
          const targetX = h.targetX ?? h.sprite.x;
          const targetY = h.targetY ?? h.sprite.y;
          h.sprite.x += (targetX - h.sprite.x) * speed;
          h.sprite.y += (targetY - h.sprite.y) * speed;
        }
      });
      this.detectCollisions();
    });
  }

  addHuman(human: HumanOutput): void {
    if (!this.app || !this.app.screen) {
      this.pendingHumans.push(human);
      return;
    }

    if (this.humanSprites.find(h => h.human.id === human.id)) {
      return;
    }

    this.createHumanSprite(human);
  }

  updateHuman(human: HumanOutput): void {
    if (!this.app || !this.app.screen) {
      return;
    }

    const existingHuman = this.humanSprites.find(h => h.human.id === human.id);
    if (existingHuman) {
      existingHuman.targetX = (human.x ?? 0) * this.app.screen.width;
      existingHuman.targetY = (human.y ?? 0) * this.app.screen.height;
      existingHuman.human = human;
    }
  }

  private createHumanSprite(human: HumanOutput): void {
    if (!this.app || !this.app.screen || !this.pixiLib) {
      return;
    }

    const PIXI = this.pixiLib;
    const emoticon = this.getPersonalityFace(human);

    const style = new PIXI.TextStyle({
      fontSize: 18,
      fontWeight: 'bold',
      fontFamily: 'Arial, sans-serif'
    });

    const text = new PIXI.Text({ text: emoticon, style });
    text.anchor.set(0.5);
    text.x = (human.x ?? 0) * this.app.screen.width;
    text.y = (human.y ?? 0) * this.app.screen.height;

    text.eventMode = 'static';
    text.cursor = 'pointer';

    let originalScale = 1;
    text.on('pointerover', () => {
      originalScale = text.scale.x;
      text.scale.set(originalScale * 1.3);
    });

    text.on('pointerout', () => {
      text.scale.set(originalScale);
    });

    this.app.stage.addChild(text);
    this.humanSprites.push({ human, sprite: text });
  }

  private getPersonalityFace(human: HumanOutput): string {
    const traits = [
      { name: 'CREATIVE', value: human.creativity ?? 0, face: '🤔' },
      { name: 'INTELLECTUAL', value: human.intellect ?? 0, face: '🧐' },
      { name: 'SOCIABLE', value: human.sociability ?? 0, face: '😊' },
      { name: 'PRACTICAL', value: human.practicality ?? 0, face: '😐' }
    ];

    const dominant = traits.reduce((prev, current) =>
      (prev.value > current.value) ? prev : current
    );

    return dominant.face;
  }

  private detectCollisions(): void {
    for (let i = 0; i < this.humanSprites.length; i++) {
      const a = this.humanSprites[i];
      for (let j = i + 1; j < this.humanSprites.length; j++) {
        const b = this.humanSprites[j];

        const dx = a.sprite.x - b.sprite.x;
        const dy = a.sprite.y - b.sprite.y;
        const distance = Math.sqrt(dx * dx + dy * dy);

        const minDist = 10;
        if (distance < minDist) {
          // Collision detected - can be handled here if needed
        }
      }
    }
  }

  private processPendingHumans(): void {
    this.pendingHumans.forEach(human => {
      if (!this.humanSprites.find(h => h.human.id === human.id)) {
        this.createHumanSprite(human);
      }
    });
    this.pendingHumans = [];
  }

  destroy(): void {
    this.resizeObserver?.disconnect();
    this.resizeObserver = undefined;

    this.humanSprites.forEach(human => {
      if (human.sprite && !human.sprite.destroyed) {
        if (human.sprite.parent) {
          human.sprite.parent.removeChild(human.sprite);
        }
        human.sprite.destroy({ children: true });
      }
    });
    this.humanSprites = [];

    if (this.app) {
      this.app.destroy(true, { children: true });
    }
  }
}
