import { z } from 'zod';

const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).+$/;

export const loginSchema = z.object({
  username: z
    .string()
    .min(1, 'Username is required')
    .min(3, 'Username must be between 3 and 50 characters')
    .max(50, 'Username must be between 3 and 50 characters'),
  password: z
    .string()
    .min(1, 'Password is required')
    .min(8, 'Password must be at least 8 characters')
    .max(100, 'Password must be at most 100 characters')
    .regex(
      passwordRegex,
      'Password must contain uppercase, lowercase, number and special character',
    ),
});

export const registerSchema = loginSchema;

export const createDocumentSchema = z.object({
  title: z
    .string()
    .min(1, 'Title is required')
    .min(3, 'Title must be between 3 and 255 characters')
    .max(255, 'Title must be between 3 and 255 characters'),
  content: z.string().min(1, 'Document content is required'),
});

export const createVersionSchema = z.object({
  documentId: z.string().uuid('Valid document ID is required'),
  content: z.string().min(1, 'Version content is required'),
});

export const commentSchema = z.object({
  content: z
    .string()
    .min(1, 'Comment content is required')
    .min(5, 'Comment must be between 5 and 1000 characters')
    .max(1000, 'Comment must be between 5 and 1000 characters'),
});
