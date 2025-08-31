# 🛒 DealSpy - AI-Powered Price Tracking Application

<div align="center">
  
  
  
  
  
  
  
  **Never miss a deal again! Track product prices across multiple e-commerce platforms with AI-powered intelligence.**

</div>

***

## 🌟 What is DealSpy?

DealSpy is an innovative application that leverages **Google's Gemini AI** to track product prices across major Indian e-commerce platforms. Users get notified instantly when prices drop on products they're interested in.

***

## ✨ Key Features

- 🤖 **AI-Powered Price Discovery** - Intelligent search across Amazon India, Flipkart, Myntra, Snapdeal, and more
- 📱 **Real-time Push Notifications** - Instant alerts for price drops using Firebase Cloud Messaging  
- 👁️ **Smart Watchlist** - Track multiple products with personalized monitoring
- 💾 **Save for Later** - Bookmark products for future reference
- 🔐 **Secure Authentication** - Firebase-powered user authentication

***

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| **Backend** | Java 21 + Spring Boot 3.5.3 |
| **Database** | PostgreSQL 17.5 |
| **AI Integration** | Google Gemini 2.5 Flash API |
| **Authentication** | Firebase Auth |
| **Notifications** | Firebase Cloud Messaging |
| **Deployment** | Docker + Render.com |

***

## 📱 API Endpoints

### 👤 **User Management**
- `GET /profile` - Fetch user profile with watchlist and saved products

### 👁️ **Watchlist Management**
- `GET /watchlist` - Get user's current watchlist
- `POST /watchlist` - Add a product to watchlist
- `DELETE /watchlist/{productName}` - Remove product from watchlist

### 💾 **Save for Later**
- `GET /saveforlater` - Fetch products saved for later
- `POST /saveforlater` - Save a product for future reference
- `DELETE /saveforlater/{productName}` - Remove product from saved list

### 🛍️ **Product Management**
- `POST /products/update-prices` - Trigger manual price update for all products

***

## 🎯 **How It Works**

1. **🔍 Smart Search** - Gemini AI searches across multiple e-commerce platforms
2. **📊 Price Comparison** - Finds the lowest available prices automatically  
3. **⏰ Continuous Monitoring** - Tracks price changes every 30 minutes
4. **📱 Instant Alerts** - Sends push notifications when prices drop
5. **🎯 Personalized Experience** - Manage watchlist and saved products

***

## 🏆 **Why DealSpy?**

✅ **AI-Powered** - No manual price checking across multiple sites  
✅ **Always Updated** - Real-time price monitoring with scheduled updates  
✅ **Never Miss Deals** - Instant notifications for price drops  
✅ **Secure & Reliable** - Enterprise-grade authentication and data protection  
✅ **Easy to Use** - Simple REST API for seamless integration  

***

<div align="center">
  
  **⭐ Star this repo if you find it helpful!**
  
  **Built with ❤️ for deal hunters**
  
  *DealSpy helps users effortlessly track prices with AI-powered search, personalized watchlists, and instant notifications to never miss great deals.*
  
</div>
