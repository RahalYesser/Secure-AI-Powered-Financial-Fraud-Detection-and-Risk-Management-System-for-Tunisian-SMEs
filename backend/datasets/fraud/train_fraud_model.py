#!/usr/bin/env python3
"""
Fraud Detection Model Training Script
Trains a Random Forest model on the fraud dataset and exports to ONNX format
"""

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score, precision_recall_curve
import joblib
import json
import os

# For ONNX conversion
try:
    from skl2onnx import convert_sklearn
    from skl2onnx.common.data_types import FloatTensorType
    import onnx
    ONNX_AVAILABLE = True
except ImportError:
    print("WARNING: skl2onnx not installed. Install with: pip install skl2onnx onnx")
    ONNX_AVAILABLE = False

# Configuration
DATA_FILE = 'fraud_dataset.csv'
MODELS_DIR = '../../src/main/resources/models'
TEST_SIZE = 0.2
RANDOM_STATE = 42

# Model hyperparameters
N_ESTIMATORS = 150
MAX_DEPTH = 15
MIN_SAMPLES_SPLIT = 20
MIN_SAMPLES_LEAF = 10

def create_dirs():
    """Create necessary directories"""
    os.makedirs(MODELS_DIR, exist_ok=True)
    print(f"✓ Models directory: {MODELS_DIR}")

def load_and_explore_data():
    """Load and explore the dataset"""
    print("\n" + "="*60)
    print("STEP 1: Loading Dataset")
    print("="*60)
    
    df = pd.read_csv(DATA_FILE)
    print(f"✓ Dataset loaded: {df.shape[0]} rows, {df.shape[1]} columns")
    print(f"\nColumns: {', '.join(df.columns.tolist())}")
    print(f"\nFirst few rows:")
    print(df.head())
    
    # Check for missing values
    missing = df.isnull().sum()
    if missing.any():
        print(f"\n⚠ Missing values found:")
        print(missing[missing > 0])
    else:
        print("\n✓ No missing values")
    
    # Class distribution
    fraud_count = df['is_fraud'].sum()
    fraud_pct = (fraud_count / len(df)) * 100
    print(f"\nClass Distribution:")
    print(f"  Legitimate: {len(df) - fraud_count} ({100-fraud_pct:.2f}%)")
    print(f"  Fraud: {fraud_count} ({fraud_pct:.2f}%)")
    
    return df

def feature_engineering(df):
    """Engineer features from the dataset"""
    print("\n" + "="*60)
    print("STEP 2: Feature Engineering")
    print("="*60)
    
    df = df.copy()
    
    # Numeric transformations
    df['amount_log'] = np.log1p(df['amount'])
    df['amount_sqrt'] = np.sqrt(df['amount'])
    
    # Time features
    df['hour_sin'] = np.sin(2 * np.pi * df['hour'] / 24)
    df['hour_cos'] = np.cos(2 * np.pi * df['hour'] / 24)
    df['is_night'] = ((df['hour'] >= 0) & (df['hour'] <= 6) | (df['hour'] >= 22)).astype(int)
    df['is_business_hours'] = ((df['hour'] >= 9) & (df['hour'] <= 17)).astype(int)
    
    # Encode categorical features
    encoders = {}
    
    print("\nEncoding categorical features:")
    for col in ['transaction_type', 'merchant_category', 'country']:
        le = LabelEncoder()
        df[f'{col}_encoded'] = le.fit_transform(df[col])
        encoders[col] = le
        print(f"  {col}: {len(le.classes_)} unique values -> {list(le.classes_)[:5]}...")
    
    # Save encoders
    for name, encoder in encoders.items():
        joblib.dump(encoder, f'{MODELS_DIR}/encoder_{name}.pkl')
    
    print(f"\n✓ Saved {len(encoders)} label encoders")
    
    # Risk score interactions
    df['risk_score_product'] = df['device_risk_score'] * df['ip_risk_score']
    df['risk_score_avg'] = (df['device_risk_score'] + df['ip_risk_score']) / 2
    df['risk_score_max'] = df[['device_risk_score', 'ip_risk_score']].max(axis=1)
    
    print(f"\n✓ Created {df.shape[1] - 10} engineered features")
    
    return df, encoders

def prepare_features(df):
    """Select and prepare features for model training"""
    print("\n" + "="*60)
    print("STEP 3: Feature Selection")
    print("="*60)
    
    # Feature columns (matching the order that will be used in Java)
    feature_columns = [
        # Amount features
        'amount',
        'amount_log',
        'amount_sqrt',
        
        # Time features
        'hour',
        'hour_sin',
        'hour_cos',
        'is_night',
        'is_business_hours',
        
        # Categorical encoded features
        'transaction_type_encoded',
        'merchant_category_encoded',
        'country_encoded',
        
        # Risk scores
        'device_risk_score',
        'ip_risk_score',
        'risk_score_product',
        'risk_score_avg',
        'risk_score_max'
    ]
    
    print(f"Selected {len(feature_columns)} features:")
    for i, feat in enumerate(feature_columns, 1):
        print(f"  {i:2d}. {feat}")
    
    X = df[feature_columns].values
    y = df['is_fraud'].values
    
    print(f"\n✓ Feature matrix: {X.shape}")
    print(f"✓ Target vector: {y.shape}")
    
    return X, y, feature_columns

def train_model(X_train, y_train):
    """Train Random Forest classifier"""
    print("\n" + "="*60)
    print("STEP 4: Model Training")
    print("="*60)
    
    print(f"\nTraining Random Forest:")
    print(f"  n_estimators: {N_ESTIMATORS}")
    print(f"  max_depth: {MAX_DEPTH}")
    print(f"  min_samples_split: {MIN_SAMPLES_SPLIT}")
    print(f"  min_samples_leaf: {MIN_SAMPLES_LEAF}")
    
    model = RandomForestClassifier(
        n_estimators=N_ESTIMATORS,
        max_depth=MAX_DEPTH,
        min_samples_split=MIN_SAMPLES_SPLIT,
        min_samples_leaf=MIN_SAMPLES_LEAF,
        random_state=RANDOM_STATE,
        n_jobs=-1,
        class_weight='balanced',  # Handle imbalanced dataset
        verbose=1
    )
    
    model.fit(X_train, y_train)
    
    print("\n✓ Model training completed")
    
    return model

def evaluate_model(model, X_train, X_test, y_train, y_test, feature_columns):
    """Evaluate model performance"""
    print("\n" + "="*60)
    print("STEP 5: Model Evaluation")
    print("="*60)
    
    # Training performance
    train_score = model.score(X_train, y_train)
    print(f"\nTrain Accuracy: {train_score:.4f}")
    
    # Test performance
    test_score = model.score(X_test, y_test)
    print(f"Test Accuracy: {test_score:.4f}")
    
    # Predictions
    y_pred = model.predict(X_test)
    y_pred_proba = model.predict_proba(X_test)[:, 1]
    
    # Classification report
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred, target_names=['Legitimate', 'Fraud']))
    
    # Confusion matrix
    cm = confusion_matrix(y_test, y_pred)
    print("\nConfusion Matrix:")
    print(f"                 Predicted")
    print(f"               Legit  Fraud")
    print(f"Actual Legit   {cm[0,0]:5d}  {cm[0,1]:5d}")
    print(f"       Fraud   {cm[1,0]:5d}  {cm[1,1]:5d}")
    
    # ROC AUC
    roc_auc = roc_auc_score(y_test, y_pred_proba)
    print(f"\nROC AUC Score: {roc_auc:.4f}")
    
    # Feature importance
    feature_importance = pd.DataFrame({
        'feature': feature_columns,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)
    
    print("\nTop 10 Feature Importances:")
    for idx, row in feature_importance.head(10).iterrows():
        print(f"  {row['feature']:30s} {row['importance']:.4f}")
    
    # Save feature importance
    feature_importance.to_csv(f'{MODELS_DIR}/feature_importance.csv', index=False)
    
    return {
        'train_accuracy': float(train_score),
        'test_accuracy': float(test_score),
        'roc_auc': float(roc_auc),
        'confusion_matrix': cm.tolist(),
        'feature_importance': feature_importance.to_dict('records')
    }

def save_scaler_and_model(scaler, model, feature_columns, metrics):
    """Save scaler and model artifacts"""
    print("\n" + "="*60)
    print("STEP 6: Saving Model Artifacts")
    print("="*60)
    
    # Save scaler
    scaler_path = f'{MODELS_DIR}/scaler.pkl'
    joblib.dump(scaler, scaler_path)
    print(f"✓ Scaler saved: {scaler_path}")
    
    # Save model (sklearn format)
    model_path = f'{MODELS_DIR}/fraud_model.pkl'
    joblib.dump(model, model_path)
    print(f"✓ Model saved: {model_path}")
    
    # Extract scaler parameters for Java
    scaler_params = {
        'means': scaler.mean_.tolist(),
        'stds': scaler.scale_.tolist(),
        'n_features': len(feature_columns)
    }
    
    with open(f'{MODELS_DIR}/scaler_params.json', 'w') as f:
        json.dump(scaler_params, f, indent=2)
    print(f"✓ Scaler parameters saved: {MODELS_DIR}/scaler_params.json")
    
    # Save metadata
    metadata = {
        'n_features': len(feature_columns),
        'feature_names': feature_columns,
        'metrics': metrics,
        'model_config': {
            'n_estimators': N_ESTIMATORS,
            'max_depth': MAX_DEPTH,
            'min_samples_split': MIN_SAMPLES_SPLIT,
            'min_samples_leaf': MIN_SAMPLES_LEAF
        }
    }
    
    with open(f'{MODELS_DIR}/model_metadata.json', 'w') as f:
        json.dump(metadata, f, indent=2)
    print(f"✓ Metadata saved: {MODELS_DIR}/model_metadata.json")

def convert_to_onnx(model, feature_columns):
    """Convert sklearn model to ONNX format"""
    print("\n" + "="*60)
    print("STEP 7: ONNX Conversion")
    print("="*60)
    
    if not ONNX_AVAILABLE:
        print("✗ ONNX conversion skipped - skl2onnx not installed")
        print("  Install with: pip install skl2onnx onnx")
        return False
    
    try:
        # Define input type
        initial_type = [('float_input', FloatTensorType([None, len(feature_columns)]))]
        
        # Convert to ONNX
        print("\nConverting model to ONNX format...")
        onnx_model = convert_sklearn(
            model,
            initial_types=initial_type,
            target_opset=12
        )
        
        # Save ONNX model
        onnx_path = f'{MODELS_DIR}/fraud_detection.onnx'
        with open(onnx_path, "wb") as f:
            f.write(onnx_model.SerializeToString())
        
        print(f"✓ ONNX model saved: {onnx_path}")
        
        # Verify ONNX model
        onnx_model_check = onnx.load(onnx_path)
        onnx.checker.check_model(onnx_model_check)
        print("✓ ONNX model validation passed")
        
        return True
    except Exception as e:
        print(f"✗ ONNX conversion failed: {e}")
        return False

def main():
    """Main training pipeline"""
    print("\n" + "="*60)
    print("FRAUD DETECTION MODEL TRAINING")
    print("="*60)
    
    # Create directories
    create_dirs()
    
    # Load data
    df = load_and_explore_data()
    
    # Feature engineering
    df, encoders = feature_engineering(df)
    
    # Prepare features
    X, y, feature_columns = prepare_features(df)
    
    # Split data
    print("\n" + "="*60)
    print("Splitting Data")
    print("="*60)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=TEST_SIZE, random_state=RANDOM_STATE, stratify=y
    )
    print(f"Train set: {X_train.shape[0]} samples")
    print(f"Test set: {X_test.shape[0]} samples")
    
    # Scale features
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    print("✓ Features scaled")
    
    # Train model
    model = train_model(X_train_scaled, y_train)
    
    # Evaluate
    metrics = evaluate_model(model, X_train_scaled, X_test_scaled, 
                            y_train, y_test, feature_columns)
    
    # Save artifacts
    save_scaler_and_model(scaler, model, feature_columns, metrics)
    
    # Convert to ONNX
    onnx_success = convert_to_onnx(model, feature_columns)
    
    # Final summary
    print("\n" + "="*60)
    print("TRAINING COMPLETE!")
    print("="*60)
    print(f"\n✓ Sklearn model: {MODELS_DIR}/fraud_model.pkl")
    print(f"✓ Scaler: {MODELS_DIR}/scaler.pkl")
    print(f"✓ Metadata: {MODELS_DIR}/model_metadata.json")
    if onnx_success:
        print(f"✓ ONNX model: {MODELS_DIR}/fraud_detection.onnx")
    print(f"\nTest Accuracy: {metrics['test_accuracy']:.4f}")
    print(f"ROC AUC Score: {metrics['roc_auc']:.4f}")
    print("\nNext steps:")
    print("1. Check {MODELS_DIR}/model_metadata.json for feature order")
    print("2. Update Java fraud detectors with correct feature extraction")
    print("3. Restart Spring Boot application to load the ONNX model")

if __name__ == '__main__':
    main()
