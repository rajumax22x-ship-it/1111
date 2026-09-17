# MAYA AI Project

## Project Overview and Purpose
The MAYA AI Project is an advanced AI platform designed to leverage Google Gemini capabilities to provide an interactive agent experience. The application features a robust backend built with Python's FastAPI, focusing on AI agent orchestration, data management with PostgreSQL (including vector embeddings), and secure authentication. The frontend offers a dynamic and responsive user interface for interacting with the AI functionalities.

The core purpose of MAYA is to:
- Provide an intuitive chat interface for users to interact with AI agents.
- Showcase various AI features and capabilities.
- Manage user data and AI-related information efficiently using a PostgreSQL database with vector search extensions.
- Ensure a secure and scalable platform for AI-driven applications.

---

## Technical Stack

### Backend
- **Language**: Python
- **Web Framework**: FastAPI
- **ASGI Server**: Uvicorn
- **AI/ML & LLM Orchestration**: LangChain (`langchain-google-genai` for Google Gemini integration), LangGraph
- **Database**: PostgreSQL (SQLAlchemy, asyncpg, psycopg2-binary)
- **Vector Database Extension**: pgvector
- **Database Migrations**: Alembic
- **Authentication**: `python-jose` (JWT), `passlib` (bcrypt)
- **Environment Management**: `python-dotenv`
- **HTTP Client**: `httpx`
- **Web Search Integration**: `tavily-python`
- **Data Validation/Settings**: Pydantic, Pydantic-settings

### Frontend
- **Language**: TypeScript
- **Framework**: React with Vite
- **State Management**: Zustand
- **Routing**: React Router DOM
- **Styling**: Tailwind CSS, PostCSS, Autoprefixer
- **Icons**: Lucide React
- **Markdown Rendering**: React Markdown
- **HTTP Client**: Axios

---

## Installation and Setup

### Prerequisites
- Python 3.8+
- Node.js (LTS version recommended)
- PostgreSQL database server

### Backend Setup
```bash
cd backend
python -m venv venv
# On Windows: .\venv\Scripts\activate
# On macOS/Linux: source venv/bin/activate
pip install -r requirements.txt

cp .env.example .env
# Configure your .env variables (GEMINI_API_KEY, DATABASE_URL, etc.)

alembic upgrade head
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

---

## Environment Variables

### Backend (`backend/.env`)
- `GEMINI_API_KEY`: Google Gemini API key.
- `DATABASE_URL`: PostgreSQL connection string (`postgresql+asyncpg://user:pass@host:port/dbname`).
- `TAVILY_API_KEY`: Tavily API key for web search.
- `SECRET_KEY`: JWT signing secret key.
- `ALGORITHM`: JWT hashing algorithm (default: HS256).
- `ACCESS_TOKEN_EXPIRE_MINUTES`: Token expiration time (default: 30).

### Frontend (`frontend/.env`)
- `VITE_API_BASE_URL`: Backend API base URL (`http://localhost:8000`).

---

## API Documentation
FastAPI provides automatic interactive documentation:
- **Swagger UI**: `http://localhost:8000/docs`
- **ReDoc**: `http://localhost:8000/redoc`
