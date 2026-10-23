import json
import os
import urllib.request
import urllib.error

def lambda_handler(event, context):
    try:
        # Retrieve the API key from environment variables
        api_key = os.environ.get("OPENAI_API_KEY")
        if not api_key:
            raise ValueError("OPENAI_API_KEY environment variable is not set")
            
        # Parse the incoming JSON payload (handles API Gateway wrapping)
        body = event.get('body')
        if isinstance(body, str):
            body = json.loads(body)
        elif not body:
            body = event
            
        # Extract the required fields
        image_base64 = body.get('image')
        position = body.get('position')
        prompt = body.get('prompt')
        
        if not all([image_base64, position, prompt]):
            return json.dumps({'error': 'Missing required fields: image, position, or prompt'})
        
        # Construct the payload for the OpenAI REST API
        openai_url = "https://api.openai.com/v1/chat/completions"
        combined_text = f"{prompt}\n\nContext - Position: {position}"
        
        payload = {
            "model": "gpt-6-luna",
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "text", 
                            "text": combined_text
                        },
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{image_base64}"
                            }
                        }
                    ]
                }
            ],
            "max_completion_tokens": 4096
        }
        
        # Prepare the HTTP request
        req = urllib.request.Request(
            openai_url,
            data=json.dumps(payload).encode('utf-8'),
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {api_key}"
            },
            method="POST"
        )
        
        # Execute the request
        with urllib.request.urlopen(req) as response:
            response_data = json.loads(response.read().decode('utf-8'))
            
        # Extract the AI's text response
        result_text = response_data['choices'][0]['message']['content']
        
        return json.dumps(json.loads(result_text))
        
    except urllib.error.HTTPError as e:
        # Handle specific HTTP errors from OpenAI
        error_msg = e.read().decode('utf-8')
        return json.dumps({'error': f"OpenAI API Error: {error_msg}"})
    except Exception as e:
        # Handle general errors
        return json.dumps({'error': str(e)})