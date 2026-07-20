"use client";

import { useEffect } from "react";
import { installFetchInterceptor } from "@/utils/fetchInterceptor";

export default function FetchInterceptorInit() {
  useEffect(() => {
    installFetchInterceptor();
  }, []);

  return null;
}
