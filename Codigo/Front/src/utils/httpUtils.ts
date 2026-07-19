export const getAuthHeaders = (): HeadersInit => {
  return {
    "Content-Type": "application/json",
  };
};

export async function fetchWithAuth(url: string, options: RequestInit = {}) {
  return fetch(url, {
    ...options,
    credentials: "include",
  });
}

export const getAuthHeadersForFormData = (): HeadersInit => {
  return {};
};
