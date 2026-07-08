from core.firebase_service import FirebaseService

firebase = FirebaseService()

firebase.initialize()

firebase.update_status()

print("Firebase OK")