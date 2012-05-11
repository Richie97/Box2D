package com.richiegames.squirrels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import org.cocos2d.actions.UpdateCallback;
import org.cocos2d.actions.instant.CCCallFuncN;
import org.cocos2d.actions.interval.CCAnimate;
import org.cocos2d.actions.interval.CCMoveTo;
import org.cocos2d.actions.interval.CCSequence;
import org.cocos2d.config.ccMacros;
import org.cocos2d.layers.CCColorLayer;
import org.cocos2d.layers.CCScene;
import org.cocos2d.nodes.CCAnimation;
import org.cocos2d.nodes.CCDirector;
import org.cocos2d.nodes.CCNode;
import org.cocos2d.nodes.CCSprite;
import org.cocos2d.types.CGPoint;
import org.cocos2d.types.CGSize;
import org.cocos2d.types.ccColor4B;

import android.view.MotionEvent;

import java.util.Iterator;
import java.util.Random;

public class Box2DScene extends CCScene {

    protected static final float PTM_RATIO = 32.0f;

    static class Box2DLayer extends CCColorLayer {

        protected final World _world;
        protected static Body _body;
        protected static CCSprite _ball, _bee;

            public Box2DLayer(ccColor4B color) {
            super(color);
                // Get window size
                CGSize s = CCDirector.sharedDirector().winSize();

                // Use scaled width and height so that our boundaries always match the current screen
                float scaledWidth = s.width/PTM_RATIO;
                float scaledHeight = s.height/PTM_RATIO;

                // Create sprite and add it to the layer
                _ball = CCSprite.sprite("Squirrel.png");
                _ball.setPosition(CGPoint.make(100, 100));



                // Create a world
                Vector2 gravity =  new Vector2(0.0f,-10.00f);
                boolean doSleep = true;
                _world = new World(gravity, doSleep);

                // Create edges around the entire screen
                // Define the ground body.
                BodyDef bxGroundBodyDef =  new BodyDef();
                bxGroundBodyDef. position.set(0.0f, 0.0f);

                // The body is also added to the world.
                Body groundBody =  _world.createBody(bxGroundBodyDef);

                // Define the ground box shape.
                PolygonShape groundBox =  new PolygonShape();
                Vector2 bottomLeft =  new Vector2(0f,0f);
                Vector2 topLeft =  new Vector2(0f,scaledHeight);
                Vector2 topRight =  new Vector2(scaledWidth,scaledHeight);
                Vector2 bottomRight =  new Vector2(scaledWidth,0f);
                // bottom
                groundBox.setAsEdge(bottomLeft, bottomRight);
                groundBody.createFixture(groundBox,0);
                // top
                groundBox.setAsEdge(topLeft, topRight);
                groundBody.createFixture(groundBox,0);
                // left
                groundBox.setAsEdge(topLeft, bottomLeft);
                groundBody.createFixture(groundBox,0);
                // right
                groundBox.setAsEdge(topRight, bottomRight);
                groundBody.createFixture(groundBox,0);

                // Create ball body and shape
                BodyDef ballBodyDef = new BodyDef();
                ballBodyDef.type = BodyDef.BodyType.DynamicBody;
                ballBodyDef.position.set(100/PTM_RATIO, 100/PTM_RATIO);
                Body ballBody = _world.createBody(ballBodyDef);
                ballBody.setUserData(_ball);
                CircleShape circle = new CircleShape();
                circle.setRadius(26.0f/PTM_RATIO);
                FixtureDef ballShapeDef = new FixtureDef();
                ballShapeDef.shape = circle;
                ballShapeDef.density = 100.0f;
                ballShapeDef.friction = 0.0f;
                ballShapeDef.restitution = 0.4f;
                ballBody.createFixture(ballShapeDef);

                this.addChild(_ball);
                // Schedule tick
                schedule(tickCallback);
                this.setIsAccelerometerEnabled(true);
                this.setIsTouchEnabled(true);

                this.schedule("cloudLogic", 1.0f);

        }
        private UpdateCallback tickCallback = new UpdateCallback() {
            @Override
            public void update(float d) {
                tick(d);
            }
        };

        public synchronized void tick(float delta) {
            synchronized (_world) {
                _world.step(delta, 8, 1);
            }

            // Iterate over the bodies in the physics world
            Iterator<Body> it =  _world.getBodies();
            while(it.hasNext()) {

                Body b = it.next();
                Object userData = b.getUserData();

                if (userData != null && userData instanceof CCSprite) {

                    //Synchronize the Sprites position and rotation with the corresponding body
                    CCSprite sprite = (CCSprite)userData;
                    sprite.setPosition(b.getPosition(). x * PTM_RATIO, b.getPosition().y * PTM_RATIO);
                    sprite.setRotation(-1.0f * ccMacros.CC_RADIANS_TO_DEGREES(b.getAngle()));
                    if(sprite.getPosition().x  + sprite.getContentSize().getWidth()< 0){
                        _world.destroyBody(b);

                    }

                }

            }
        }

        @Override
        public void ccAccelerometerChanged(float accelX, float accelY, float accelZ) {
            // Landscape values
            Vector2 gravity =  new Vector2(accelY*3, (-accelX + 8.0f) * 15);
            _world.setGravity(gravity);
        }

        @Override
        public boolean ccTouchesMoved(MotionEvent event) {

            // Convert to CGPoint
            CGPoint location = CCDirector.sharedDirector().convertToGL(CGPoint.make(event.getX(),event.getY()));

            // Iterate over the bodies in the physics world
            Iterator<Body> it = _world.getBodies();
            while (it.hasNext()) {
                Body b = it.next();
                Object userData = b.getUserData();

                // Look for the body that has the ball for userData
                if (userData != null && userData == _ball) {
                    b.setTransform(new Vector2(location.x/PTM_RATIO, location.y/PTM_RATIO),
                            b.getAngle());
                }
            }
            return super.ccTouchesMoved(event);
        }

        protected void addTarget(){
            Random rand = new Random();
            CCSprite target = CCSprite.sprite("Cloud_sprite.png");

            // Determine where to spawn the target along the Y axis
            CGSize winSize = CCDirector.sharedDirector().displaySize();
            int minY = (int)(target.getContentSize().height / 2.0f);
            int maxY = (int)(winSize.height - target.getContentSize().height / 2.0f);
            int rangeY = maxY - minY;
            int actualY = rand.nextInt(rangeY) + minY;

            // Create the target slightly off-screen along the right edge,
            // and along a random position along the Y axis as calculated above
            CGPoint cgPoint = new CGPoint();
            cgPoint.x = winSize.width + (target.getContentSize().width / 2.0f);
            cgPoint.y = actualY;
            target.setPosition(cgPoint);
            addChild(target);
            target.setTag(1);
            // Determine speed of the target
            int minDuration = 2;
            int maxDuration = 4;
            int rangeDuration = maxDuration - minDuration;
            int actualDuration = rand.nextInt(rangeDuration) + minDuration;

            // Create the actions
            CCMoveTo actionMove = CCMoveTo.action(actualDuration, CGPoint.ccp(-target.getContentSize().width / 2.0f, actualY));
            CCCallFuncN actionMoveDone = CCCallFuncN.action(this, "spriteMoveFinished");
            CCSequence actions = CCSequence.actions(actionMove, actionMoveDone);

            target.runAction(actions);
        }

        protected void addBees(){
            Random rand = new Random();
            //CCSprite target = CCSprite.sprite("bee1.png");

            // Determine where to spawn the target along the Y axis



            _bee = CCSprite.sprite("bee1.png");
            CGSize winSize = CCDirector.sharedDirector().displaySize();
            int minY = (int)(_bee.getContentSize().height / 2.0f);
            int maxY = (int)(winSize.height - _bee.getContentSize().height / 2.0f);
            int rangeY = maxY - minY;
            int actualY = rand.nextInt(rangeY) + minY;

            CGPoint cgPoint = new CGPoint();
            cgPoint.x = winSize.width + (_bee.getContentSize().width / 2.0f);
            cgPoint.y = actualY;
            _bee.setPosition(cgPoint);
            _bee.setPosition(cgPoint);
            // Create ball body and shape
            BodyDef ballBodyDef = new BodyDef();
            ballBodyDef.type = BodyDef.BodyType.KinematicBody;
            ballBodyDef.position.set((winSize.width-_bee.getContentSize().getWidth()/2)/PTM_RATIO, actualY/PTM_RATIO);
            ballBodyDef.linearVelocity.set(-rand.nextInt(10)-3, 0f);
            Body ballBody = _world.createBody(ballBodyDef);
            ballBody.setUserData(_bee);
            CircleShape circle = new CircleShape();
            circle.setRadius(10.0f/PTM_RATIO);
            FixtureDef ballShapeDef = new FixtureDef();
            ballShapeDef.shape = circle;
            ballShapeDef.density = 10.0f;
            ballShapeDef.friction = 0.0f;
            ballShapeDef.restitution = 0.4f;
            ballBody.createFixture(ballShapeDef);

            // Create the target slightly off-screen along the right edge,
            // and along a random position along the Y axis as calculated above

            this.addChild(_bee);
            _bee.setTag(1);
            // Determine speed of the target
            int minDuration = 2;
            int maxDuration = 4;
            int rangeDuration = maxDuration - minDuration;
            int actualDuration = rand.nextInt(rangeDuration) + minDuration;

            // Create the actions
//            CCMoveTo actionMove = CCMoveTo.action(actualDuration, CGPoint.ccp(-_bee.getContentSize().width / 2.0f, actualY));
//            CCCallFuncN actionMoveDone = CCCallFuncN.action(this, "spriteMoveFinished");
//            CCSequence actions = CCSequence.actions(actionMove, actionMoveDone);
            CCAnimation flap = CCAnimation.animation("flap", 50f);
            flap.addFrame("bee1.png");
            flap.addFrame("bee2.png");
            _bee.addAnimation(flap);
            CCAnimate anim = CCAnimate.action(flap);
//            _bee.runAction(actions);
            _bee.runAction(anim);

        }

        public void spriteMoveFinished(Object sender){
            CCSprite sprite = (CCSprite)sender;
            this.removeChild(sprite, true);
        }

        public void cloudLogic(float dt){
                addTarget();
                addBees();
        }

    }
}

