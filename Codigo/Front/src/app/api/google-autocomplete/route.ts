// app/api/google-autocomplete/route.ts
import { NextRequest, NextResponse } from 'next/server';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const input = body.input;

    if (!input) {
      return NextResponse.json({ error: 'Input is required' }, { status: 400 });
    }

    const response = await fetch("https://places.googleapis.com/v1/places:autocomplete", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-Goog-Api-Key": process.env.GOOGLE_MAPS_KEY!
        },
        body: JSON.stringify({ input })
    });
    const data = await response.json();
    console.log("Google Places API response:", data); // Log da resposta da API

    return NextResponse.json(data);
  } catch (err) {
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
