#import <Foundation/Foundation.h>
@import FirebaseFirestore;

NS_ASSUME_NONNULL_BEGIN

@interface FirebaseFirestoreBridge : NSObject

/**
 * Adds an activity document for a user.
 */
+ (void)addActivityForUser:(NSString *)userId
        data:(NSDictionary *)data
        completion:(void (^)(NSError * _Nullable error))completion;

/**
 * Reads activities for a user ordered by timestamp descending.
 *
 * Each returned dictionary contains the Firestore document data
 * plus "_documentId" containing the Firestore document ID.
 */
+ (void)getActivitiesForUser:(NSString *)userId
        completion:(void (^)(NSArray<NSDictionary *> * _Nullable activities,
        NSError * _Nullable error))completion;

/**
 * Reads today's activities for a user.
 */
+ (void)getActivitiesForUser:(NSString *)userId
        fromTimestamp:(int64_t)timestamp
        completion:(void (^)(NSArray<NSDictionary *> * _Nullable activities,
        NSError * _Nullable error))completion;

@end

        NS_ASSUME_NONNULL_END