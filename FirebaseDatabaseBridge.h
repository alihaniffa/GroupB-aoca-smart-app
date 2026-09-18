#import <Foundation/Foundation.h>
@import FirebaseDatabase;

NS_ASSUME_NONNULL_BEGIN

@interface FirebaseDatabaseBridge : NSObject

+ (void)observeValueOnceForQuery:(FIRDatabaseQuery *)query
        completion:(void (^)(FIRDataSnapshot * _Nullable snapshot))completion;

@end

        NS_ASSUME_NONNULL_END