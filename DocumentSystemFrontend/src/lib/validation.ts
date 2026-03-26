export function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value.trim());
}

export function validateUsername(value: string): string | null {
  const normalized = value.trim();
  if (normalized.length < 3) return "Username must be at least 3 characters.";
  if (normalized.length > 50) return "Username must be at most 50 characters.";
  return null;
}

export function validatePassword(value: string): string | null {
  if (value.length < 6) return "Password must be at least 6 characters.";
  if (value.length > 100) return "Password must be at most 100 characters.";
  return null;
}
