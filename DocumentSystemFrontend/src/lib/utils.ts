import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(value?: string) {
  if (!value) return '-';
  return new Date(value).toLocaleString();
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  window.URL.revokeObjectURL(url);
}

export function htmlToPlainText(content: string) {
  const parser = new DOMParser();
  const document = parser.parseFromString(content, 'text/html');
  const body = document.body;

  body.querySelectorAll('br').forEach((node) => {
    node.replaceWith('\n');
  });

  body.querySelectorAll('p, div, li, h1, h2, h3, h4, h5, h6, blockquote, pre').forEach((node) => {
    node.append('\n');
  });

  return body.textContent?.replace(/\n{3,}/g, '\n\n').trim() || '';
}

export function toFileFromHtml(content: string, baseName: string) {
  return new File([content], `${baseName}.html`, {
    type: 'text/html;charset=utf-8',
  });
}

export function toTextFile(content: string, baseName: string) {
  const plainText = htmlToPlainText(content);

  return new File([plainText], `${baseName}.txt`, {
    type: 'text/plain',
  });
}
