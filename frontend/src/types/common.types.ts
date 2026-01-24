export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}

export interface ValidationErrorResponse extends ErrorResponse {
  errors: Record<string, string>;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SearchParams {
  query?: string;
  page?: number;
  size?: number;
  sort?: string;
}
