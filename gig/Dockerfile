# Use the official Python image
FROM python:3.11-slim

# Set the working directory in the container
WORKDIR /app

# Copy the requirements file and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of application code
COPY . /app

# Expose the port (optional)
# Cloud Run injects the PORT environment variable
ENV PORT 8085
EXPOSE 8085


CMD uvicorn gigAgent.whatsapp_bidi_server:app --host 0.0.0.0 --port $PORT