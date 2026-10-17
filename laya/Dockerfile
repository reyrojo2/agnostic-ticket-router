FROM python:3.11-slim
ENV PYTHONUNBUFFERED=1
RUN pip install --no-cache-dir "laya[serve]"
EXPOSE 8000
CMD ["laya-serve"]
